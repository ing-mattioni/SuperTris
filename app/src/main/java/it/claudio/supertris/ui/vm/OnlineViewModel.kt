package it.claudio.supertris.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.claudio.supertris.core.GameMode
import it.claudio.supertris.core.GameState
import it.claudio.supertris.core.Move
import it.claudio.supertris.core.SuperTrisRules
import it.claudio.supertris.data.GameRepository
import it.claudio.supertris.data.buildHistoryEntry
import it.claudio.supertris.net.GameSummary
import it.claudio.supertris.net.JoinResult
import it.claudio.supertris.net.OnlineRoom
import it.claudio.supertris.net.OnlineRoomClient
import it.claudio.supertris.net.buildLocalState
import it.claudio.supertris.net.toSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class OnlineFailure { NOT_FOUND, FULL, VERSION, NETWORK, ROOM_GONE }

/** Emoji ricevuta dall'avversario; [seq] distingue invii ripetuti della stessa emoji. */
data class EmojiEvent(val emoji: String, val seq: Long)

sealed interface OnlineLobbyState {
    data object Idle : OnlineLobbyState
    data object Working : OnlineLobbyState
    data class WaitingGuest(val code: String) : OnlineLobbyState
    data object InGame : OnlineLobbyState
    data class Failed(val reason: OnlineFailure) : OnlineLobbyState
}

/**
 * Flusso "a distanza": crea/unisciti a una stanza Firestore, osserva il
 * documento e ricostruisce lo stato applicando le mosse (stesso principio
 * deterministico di Nearby). L'host esegue il reset della rivincita.
 */
class OnlineViewModel(
    private val repo: GameRepository,
    private val client: OnlineRoomClient,
) : ViewModel() {

    private val _lobbyState = MutableStateFlow<OnlineLobbyState>(OnlineLobbyState.Idle)
    val lobbyState: StateFlow<OnlineLobbyState> = _lobbyState

    private val _gameUi = MutableStateFlow(GameUiState(gameMode = GameMode.ONLINE))
    val gameUi: StateFlow<GameUiState> = _gameUi

    private val _opponentOffline = MutableStateFlow(false)
    val opponentOffline: StateFlow<Boolean> = _opponentOffline

    private val _roomGone = MutableStateFlow(false)
    val roomGone: StateFlow<Boolean> = _roomGone

    private val _incomingEmoji = MutableStateFlow<EmojiEvent?>(null)
    val incomingEmoji: StateFlow<EmojiEvent?> = _incomingEmoji

    val nickname: StateFlow<String> = repo.nicknameFlow
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // "Le mie partite": osservata solo mentre la lobby e' visibile.
    private val _myGames = MutableStateFlow<List<GameSummary>>(emptyList())
    val myGames: StateFlow<List<GameSummary>> = _myGames

    private var gamesJob: Job? = null
    private var observeJob: Job? = null
    private var presenceJob: Job? = null
    private var currentRoom: OnlineRoom? = null
    private var currentState: GameState? = null
    private var isHost = false
    private var roomCode: String? = null
    private var lastMovesCount = 0
    private var lastRound = -1
    private var lastPeerEmojiSeq = -1L
    private var lastEmojiSentAtMs = 0L
    private val celebrations = CelebrationTracker()

    // ---------- Lista partite ----------

    fun startGamesListener() {
        if (gamesJob != null) return
        gamesJob = viewModelScope.launch {
            val uid = runCatching { client.ensureSignedIn() }.getOrNull() ?: return@launch
            client.observeMyGames(uid).collect { rooms ->
                _myGames.value = rooms.mapNotNull { it.toSummary(uid) }
            }
        }
    }

    fun stopGamesListener() {
        gamesJob?.cancel()
        gamesJob = null
    }

    // ---------- Azioni lobby ----------

    fun createRoom(nicknameInput: String) {
        val name = nicknameInput.trim().ifBlank { "Giocatore" }
        _lobbyState.value = OnlineLobbyState.Working
        viewModelScope.launch {
            repo.saveNickname(name)
            try {
                val room = client.createRoom(name)
                isHost = true
                roomCode = room.code
                startSession(room.code)
                _lobbyState.value = OnlineLobbyState.WaitingGuest(room.code)
            } catch (e: Exception) {
                _lobbyState.value = OnlineLobbyState.Failed(OnlineFailure.NETWORK)
            }
        }
    }

    fun joinRoom(codeInput: String, nicknameInput: String) {
        val code = codeInput.uppercase().trim()
        val name = nicknameInput.trim().ifBlank { "Giocatore" }
        if (!OnlineRoomClient.isValidCodeFormat(code)) {
            _lobbyState.value = OnlineLobbyState.Failed(OnlineFailure.NOT_FOUND)
            return
        }
        _lobbyState.value = OnlineLobbyState.Working
        viewModelScope.launch {
            repo.saveNickname(name)
            try {
                when (val result = client.joinRoom(code, name)) {
                    is JoinResult.Ok -> {
                        isHost = result.room.hostUid == client.uid
                        roomCode = result.room.code
                        startSession(result.room.code)
                    }
                    JoinResult.NotFound -> _lobbyState.value = OnlineLobbyState.Failed(OnlineFailure.NOT_FOUND)
                    JoinResult.Full -> _lobbyState.value = OnlineLobbyState.Failed(OnlineFailure.FULL)
                    JoinResult.VersionMismatch -> _lobbyState.value = OnlineLobbyState.Failed(OnlineFailure.VERSION)
                }
            } catch (e: Exception) {
                _lobbyState.value = OnlineLobbyState.Failed(OnlineFailure.NETWORK)
            }
        }
    }

    /** Apre una partita dalla lista "Le mie partite" (o riprende dopo un riavvio). */
    fun openGame(code: String) {
        _lobbyState.value = OnlineLobbyState.Working
        viewModelScope.launch {
            try {
                val room = client.fetchRoomIfParticipant(code)
                if (room == null || room.status == "finished") {
                    _lobbyState.value = OnlineLobbyState.Failed(OnlineFailure.ROOM_GONE)
                } else {
                    isHost = room.hostUid == client.uid
                    roomCode = room.code
                    startSession(room.code)
                }
            } catch (e: Exception) {
                _lobbyState.value = OnlineLobbyState.Failed(OnlineFailure.NETWORK)
            }
        }
    }

    /** Host in attesa che annulla: la stanza viene eliminata. */
    fun cancelWaiting() {
        val code = roomCode
        stopSession()
        if (code != null) {
            viewModelScope.launch {
                client.leaveRoom(code, isHost = true, stillWaiting = true)
            }
        }
        _lobbyState.value = OnlineLobbyState.Idle
    }

    /** Dopo un errore, torna alla scelta iniziale. */
    fun resetToIdle() {
        stopSession()
        _lobbyState.value = OnlineLobbyState.Idle
    }

    /**
     * Uscita dalla partita: resta valida sul server entro il TTL e si
     * ritrova nella lista "Le mie partite".
     */
    fun exitGame() {
        stopSession()
        _lobbyState.value = OnlineLobbyState.Idle
    }

    // ---------- Partita ----------

    fun onTap(micro: Int, cell: Int) {
        val state = currentState ?: return
        val code = roomCode ?: return
        if (state.isGameOver()) return
        if (!SuperTrisRules.isHumanTurn(state)) return

        val move = Move(micro = micro, cell = cell)
        if (!SuperTrisRules.isLegalMove(state, move)) return

        viewModelScope.launch {
            // Se la transazione fallisce (indice non allineato), lo snapshot
            // successivo riallinea comunque la UI: nessun altro intervento.
            runCatching {
                client.sendMove(code, expectedIndex = state.moveCount, micro = micro, cell = cell, isHost = isHost)
            }
        }
    }

    fun requestRematch() {
        val room = currentRoom ?: return
        val code = roomCode ?: return
        if (currentState?.isGameOver() != true) return
        val alreadyAsked = if (isHost) room.hostRematch else room.guestRematch
        if (alreadyAsked) return
        viewModelScope.launch {
            runCatching { client.requestRematch(code, isHost) }
        }
    }

    fun sendEmoji(emoji: String) {
        if (emoji !in ALLOWED_EMOJI) return
        val code = roomCode ?: return
        val now = System.currentTimeMillis()
        if (now - lastEmojiSentAtMs < EMOJI_MIN_INTERVAL_MS) return
        lastEmojiSentAtMs = now
        viewModelScope.launch {
            runCatching { client.sendEmoji(code, isHost, emoji) }
        }
    }

    // ---------- Osservazione stanza ----------

    private fun startSession(code: String) {
        observeJob?.cancel()
        presenceJob?.cancel()
        lastMovesCount = 0
        lastRound = -1
        lastPeerEmojiSeq = -1L
        celebrations.reset()
        currentRoom = null
        currentState = null
        _opponentOffline.value = false
        _roomGone.value = false
        _incomingEmoji.value = null

        observeJob = client.observeRoom(code)
            .onEach { onRoomUpdate(it) }
            .launchIn(viewModelScope)

        presenceJob = viewModelScope.launch {
            var tick = 0
            while (isActive) {
                if (tick % 2 == 0) client.heartbeat(code, isHost)
                updatePresence()
                tick++
                delay(15_000)
            }
        }
    }

    private fun stopSession() {
        observeJob?.cancel()
        presenceJob?.cancel()
        observeJob = null
        presenceJob = null
        currentRoom = null
        currentState = null
        _opponentOffline.value = false
        _roomGone.value = false
    }

    private fun onRoomUpdate(room: OnlineRoom?) {
        if (room == null) {
            if (_lobbyState.value == OnlineLobbyState.InGame ||
                _lobbyState.value is OnlineLobbyState.WaitingGuest
            ) {
                _roomGone.value = true
            }
            return
        }
        currentRoom = room

        if (room.guestUid == null) {
            _lobbyState.value = OnlineLobbyState.WaitingGuest(room.code)
            return
        }
        if (room.status == "finished") {
            _roomGone.value = true
            return
        }

        // Emoji dell'avversario: la prima lettura fissa la baseline (niente
        // animazione per reazioni vecchie), poi ogni incremento fa scattare l'evento.
        val peerEmojiSeq = if (isHost) room.guestEmojiSeq else room.hostEmojiSeq
        val peerEmoji = if (isHost) room.guestEmoji else room.hostEmoji
        if (lastPeerEmojiSeq < 0) {
            lastPeerEmojiSeq = peerEmojiSeq
        } else if (peerEmojiSeq > lastPeerEmojiSeq) {
            lastPeerEmojiSeq = peerEmojiSeq
            if (peerEmoji != null && peerEmoji in ALLOWED_EMOJI) {
                _incomingEmoji.value = EmojiEvent(emoji = peerEmoji, seq = peerEmojiSeq)
            }
        }

        // Rivincita: quando entrambi hanno accettato, l'host resetta la stanza.
        if (isHost && room.hostRematch && room.guestRematch) {
            viewModelScope.launch { runCatching { client.restartIfBothAgreed(room.code) } }
        }

        if (room.round != lastRound) {
            lastRound = room.round
            lastMovesCount = 0
            celebrations.reset()
        }

        val state = room.buildLocalState(localIsHost = isHost)
        if (state == null) {
            // Sequenza mosse corrotta: irrecuperabile, meglio chiudere con garbo.
            _roomGone.value = true
            return
        }

        if (room.moves.size == lastMovesCount + 1) {
            val previous = room.copy(moves = room.moves.dropLast(1)).buildLocalState(localIsHost = isHost)
            val last = room.moves.last()
            if (previous != null) {
                celebrations.register(previous, state, Move(micro = last.micro, cell = last.cell))
            }
        }
        lastMovesCount = room.moves.size

        // Registrazione esito con dedup persistente ("codice#round"): copre anche
        // le partite chiuse dall'avversario mentre eri offline e riaperte dopo.
        if (state.isGameOver()) {
            val key = "${room.code}#${room.round}"
            val opponent = if (isHost) room.guestName else room.hostName
            viewModelScope.launch {
                if (repo.markOnlineResultRecorded(key)) {
                    buildHistoryEntry(state, opponentName = opponent)?.let { repo.recordFinishedGame(it) }
                }
            }
        }

        pushUi(state, room)
        updatePresence()
        _lobbyState.value = OnlineLobbyState.InGame
    }

    private fun updatePresence() {
        val room = currentRoom
        val inGame = _lobbyState.value == OnlineLobbyState.InGame
        val peerSeen = if (isHost) room?.guestSeenMs else room?.hostSeenMs
        _opponentOffline.value = inGame && peerSeen != null &&
            System.currentTimeMillis() - peerSeen > OFFLINE_AFTER_MS
    }

    private fun pushUi(state: GameState, room: OnlineRoom) {
        currentState = state
        val myRematch = if (isHost) room.hostRematch else room.guestRematch
        val peerRematch = if (isHost) room.guestRematch else room.hostRematch
        _gameUi.value = GameUiState(
            gameMode = GameMode.ONLINE,
            humanMark = state.humanMark,
            turnMark = state.turn,
            opponentName = if (isHost) room.guestName else room.hostName,
            isHumanTurn = SuperTrisRules.isHumanTurn(state),
            isAiThinking = false,
            forcedMicro = state.forcedMicro,
            cells = state.cells,
            microStatus = state.microStatus,
            macroStatus = state.macroStatus,
            isGameOver = state.isGameOver(),
            lastResolvedMicro = celebrations.lastResolvedMicro,
            lastResolvedMark = celebrations.lastResolvedMark,
            microCelebrationToken = celebrations.microCelebrationToken,
            gameOverCelebrationToken = celebrations.gameOverCelebrationToken,
            waitingRematch = myRematch && !peerRematch,
        )
    }

    override fun onCleared() {
        stopSession()
    }

    class Factory(
        private val repo: GameRepository,
        private val client: OnlineRoomClient,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return OnlineViewModel(repo, client) as T
        }
    }

    companion object {
        val ALLOWED_EMOJI = listOf("👏", "😂", "😱", "🔥", "🤔", "😎")
        private const val OFFLINE_AFTER_MS = 90_000L
        private const val EMOJI_MIN_INTERVAL_MS = 2_000L
    }
}
