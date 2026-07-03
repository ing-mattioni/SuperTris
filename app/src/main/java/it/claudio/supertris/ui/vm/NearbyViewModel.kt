package it.claudio.supertris.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.claudio.supertris.core.GameMode
import it.claudio.supertris.core.GameState
import it.claudio.supertris.core.Move
import it.claudio.supertris.core.SuperTrisRules
import it.claudio.supertris.data.GameRepository
import it.claudio.supertris.net.NearbyEvent
import it.claudio.supertris.net.NearbyTransport
import it.claudio.supertris.net.NetMessage
import it.claudio.supertris.net.PROTOCOL_VERSION
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

data class LobbyEndpoint(val endpointId: String, val name: String)

enum class LobbyFailure { CONNECTION, VERSION }

sealed interface LobbyState {
    data object Idle : LobbyState
    data object Advertising : LobbyState
    data class Discovering(val endpoints: List<LobbyEndpoint>) : LobbyState
    data class AuthConfirm(val endpointId: String, val peerName: String, val authDigits: String) : LobbyState
    data object Connecting : LobbyState
    data object InGame : LobbyState
    data class Failed(val reason: LobbyFailure) : LobbyState
}

/**
 * Gestisce l'intero flusso "due telefoni vicini": lobby (advertise/discover,
 * conferma codice), handshake Hello/GameStart e la partita vera e propria.
 * L'host e' l'autorita': in caso di disallineamento invia lo stato completo.
 */
class NearbyViewModel(
    private val transport: NearbyTransport,
    private val deviceFallbackName: String,
    private val repo: GameRepository,
) : ViewModel() {

    /** Nickname salvato, per precompilare il campo nella lobby. */
    val savedNickname: StateFlow<String> = repo.nicknameFlow
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // Nome mostrato all'altro telefono: nickname se presente, altrimenti il modello.
    private var localName: String = deviceFallbackName

    private fun applyNickname(nicknameInput: String) {
        val name = nicknameInput.trim()
        localName = name.ifBlank { deviceFallbackName }
        if (name.isNotBlank()) {
            viewModelScope.launch { repo.saveNickname(name) }
        }
    }

    private val _lobbyState = MutableStateFlow<LobbyState>(LobbyState.Idle)
    val lobbyState: StateFlow<LobbyState> = _lobbyState

    private val _gameUi = MutableStateFlow(GameUiState(gameMode = GameMode.NEARBY))
    val gameUi: StateFlow<GameUiState> = _gameUi

    private val _opponentLeft = MutableStateFlow(false)
    val opponentLeft: StateFlow<Boolean> = _opponentLeft

    private var isHost = false
    private var endpointId: String? = null
    private var peerName: String? = null
    private var helloReceived = false
    private var gameState: GameState? = null
    private var localRematch = false
    private var remoteRematch = false
    private val celebrations = CelebrationTracker()
    private val random = Random.Default

    init {
        transport.events
            .onEach { handleEvent(it) }
            .launchIn(viewModelScope)
    }

    // ---------- Azioni lobby ----------

    fun startHosting(nickname: String = "") {
        applyNickname(nickname)
        resetSession()
        isHost = true
        _lobbyState.value = LobbyState.Advertising
        transport.startAdvertising(localName)
    }

    fun startJoining(nickname: String = "") {
        applyNickname(nickname)
        resetSession()
        isHost = false
        _lobbyState.value = LobbyState.Discovering(emptyList())
        transport.startDiscovery()
    }

    fun connectTo(endpoint: LobbyEndpoint) {
        _lobbyState.value = LobbyState.Connecting
        transport.requestConnection(localName, endpoint.endpointId)
    }

    fun confirmAuth() {
        val st = _lobbyState.value as? LobbyState.AuthConfirm ?: return
        _lobbyState.value = LobbyState.Connecting
        transport.acceptConnection(st.endpointId)
    }

    fun rejectAuth() {
        val st = _lobbyState.value as? LobbyState.AuthConfirm ?: return
        transport.rejectConnection(st.endpointId)
        if (isHost) {
            _lobbyState.value = LobbyState.Advertising
        } else {
            transport.stopDiscovery()
            transport.startDiscovery()
            _lobbyState.value = LobbyState.Discovering(emptyList())
        }
    }

    /** Annulla advertising/discovery e torna alla scelta del ruolo. */
    fun leaveLobby() {
        transport.stopAll()
        transport.stopAdvertising()
        transport.stopDiscovery()
        resetSession()
    }

    /** Esce dalla partita e chiude la connessione. */
    fun quitGame() {
        transport.stopAll()
        resetSession()
    }

    fun requestRematch() {
        val st = gameState ?: return
        if (!st.isGameOver() || localRematch) return
        localRematch = true
        endpointId?.let { transport.send(it, NetMessage.RematchRequest) }
        pushUi(st)
        maybeStartRematch()
    }

    fun onTap(micro: Int, cell: Int) {
        val st = gameState ?: return
        if (st.isGameOver()) return
        if (!SuperTrisRules.isHumanTurn(st)) return

        val move = Move(micro = micro, cell = cell)
        if (!SuperTrisRules.isLegalMove(st, move)) return
        val ep = endpointId ?: return

        val next = SuperTrisRules.applyMove(st, move)
        celebrations.register(previous = st, next = next, move = move)
        gameState = next
        transport.send(ep, NetMessage.MoveMsg(moveIndex = st.moveCount, micro = micro, cell = cell))
        pushUi(next)
    }

    // ---------- Eventi dal trasporto ----------

    private fun handleEvent(event: NearbyEvent) {
        when (event) {
            is NearbyEvent.EndpointFound -> {
                val st = _lobbyState.value as? LobbyState.Discovering ?: return
                if (st.endpoints.any { it.endpointId == event.endpointId }) return
                _lobbyState.value = st.copy(endpoints = st.endpoints + LobbyEndpoint(event.endpointId, event.name))
            }

            is NearbyEvent.EndpointLost -> {
                val st = _lobbyState.value as? LobbyState.Discovering ?: return
                _lobbyState.value = st.copy(endpoints = st.endpoints.filterNot { it.endpointId == event.endpointId })
            }

            is NearbyEvent.ConnectionInitiated -> {
                endpointId = event.endpointId
                _lobbyState.value = LobbyState.AuthConfirm(
                    endpointId = event.endpointId,
                    peerName = event.peerName,
                    authDigits = event.authDigits,
                )
            }

            is NearbyEvent.Connected -> {
                if (isHost) transport.stopAdvertising() else transport.stopDiscovery()
                _lobbyState.value = LobbyState.Connecting
                transport.send(event.endpointId, NetMessage.Hello(PROTOCOL_VERSION, localName))
                maybeStartAsHost()
            }

            is NearbyEvent.ConnectionFailed -> {
                if (_lobbyState.value != LobbyState.InGame) {
                    _lobbyState.value = LobbyState.Failed(LobbyFailure.CONNECTION)
                }
            }

            is NearbyEvent.Disconnected -> {
                if (_lobbyState.value == LobbyState.InGame) {
                    _opponentLeft.value = true
                } else if (_lobbyState.value != LobbyState.Idle) {
                    _lobbyState.value = LobbyState.Failed(LobbyFailure.CONNECTION)
                }
            }

            is NearbyEvent.MessageReceived -> handleMessage(event.message)

            is NearbyEvent.OperationFailed -> {
                if (_lobbyState.value is LobbyState.Advertising ||
                    _lobbyState.value is LobbyState.Discovering ||
                    _lobbyState.value is LobbyState.Connecting
                ) {
                    _lobbyState.value = LobbyState.Failed(LobbyFailure.CONNECTION)
                }
            }
        }
    }

    private fun handleMessage(msg: NetMessage) {
        when (msg) {
            is NetMessage.Hello -> {
                if (msg.protocolVersion != PROTOCOL_VERSION) {
                    transport.stopAll()
                    _lobbyState.value = LobbyState.Failed(LobbyFailure.VERSION)
                    return
                }
                peerName = msg.playerName
                helloReceived = true
                maybeStartAsHost()
            }

            is NetMessage.GameStart -> {
                if (isHost) return
                // Accettato solo a inizio sessione o come rivincita a partita finita.
                if (gameState != null && gameState?.isGameOver() != true) return
                startLocalGame(localMark = msg.guestMark, firstTurn = msg.firstTurn)
            }

            is NetMessage.MoveMsg -> onRemoteMove(msg)

            NetMessage.SyncRequest -> if (isHost) sendSync()

            is NetMessage.StateSync -> if (!isHost) adoptSync(msg)

            NetMessage.RematchRequest -> {
                remoteRematch = true
                maybeStartRematch()
            }
        }
    }

    // ---------- Logica di partita ----------

    private fun maybeStartAsHost() {
        if (!isHost || !helloReceived || gameState != null) return
        val ep = endpointId ?: return

        val guestMark = if (random.nextBoolean()) SuperTrisRules.X else SuperTrisRules.O
        val firstTurn = if (random.nextBoolean()) SuperTrisRules.X else SuperTrisRules.O
        transport.send(ep, NetMessage.GameStart(guestMark = guestMark, firstTurn = firstTurn))
        startLocalGame(localMark = SuperTrisRules.other(guestMark), firstTurn = firstTurn)
    }

    private fun startLocalGame(localMark: Int, firstTurn: Int) {
        celebrations.reset()
        localRematch = false
        remoteRematch = false
        val st = SuperTrisRules.newRemoteGame(localMark = localMark, firstTurn = firstTurn, gameMode = GameMode.NEARBY)
        gameState = st
        pushUi(st)
        _lobbyState.value = LobbyState.InGame
    }

    private fun onRemoteMove(msg: NetMessage.MoveMsg) {
        val st = gameState ?: return
        val move = Move(micro = msg.micro, cell = msg.cell)
        val isRemoteTurn = !SuperTrisRules.isHumanTurn(st)

        if (msg.moveIndex != st.moveCount || !isRemoteTurn || !SuperTrisRules.isLegalMove(st, move)) {
            // Disallineamento: l'host manda lo stato autoritativo, il guest lo chiede.
            if (isHost) sendSync() else endpointId?.let { transport.send(it, NetMessage.SyncRequest) }
            return
        }

        val next = SuperTrisRules.applyMove(st, move)
        celebrations.register(previous = st, next = next, move = move)
        gameState = next
        pushUi(next)
    }

    private fun maybeStartRematch() {
        if (!localRematch || !remoteRematch) return
        if (isHost) {
            gameState = null
            maybeStartAsHost()
        }
        // Il guest attende il nuovo GameStart dall'host.
    }

    private fun sendSync() {
        val st = gameState ?: return
        val ep = endpointId ?: return
        transport.send(
            ep,
            NetMessage.StateSync(
                hostMark = st.humanMark,
                turn = st.turn,
                forcedMicro = st.forcedMicro,
                cells = st.cells,
                microStatus = st.microStatus,
                macroStatus = st.macroStatus,
                moveCount = st.moveCount,
            ),
        )
    }

    private fun adoptSync(msg: NetMessage.StateSync) {
        val local = gameState ?: return
        val st = local.copy(
            humanMark = SuperTrisRules.other(msg.hostMark),
            turn = msg.turn,
            forcedMicro = msg.forcedMicro,
            cells = msg.cells,
            microStatus = msg.microStatus,
            macroStatus = msg.macroStatus,
            moveCount = msg.moveCount,
        )
        gameState = st
        pushUi(st)
    }

    private fun resetSession() {
        isHost = false
        endpointId = null
        peerName = null
        helloReceived = false
        gameState = null
        localRematch = false
        remoteRematch = false
        celebrations.reset()
        _opponentLeft.value = false
        _gameUi.value = GameUiState(gameMode = GameMode.NEARBY)
        _lobbyState.value = LobbyState.Idle
    }

    private fun pushUi(state: GameState) {
        _gameUi.value = GameUiState(
            gameMode = GameMode.NEARBY,
            humanMark = state.humanMark,
            turnMark = state.turn,
            opponentName = peerName,
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
            waitingRematch = localRematch && !remoteRematch,
        )
    }

    override fun onCleared() {
        transport.stopAll()
    }

    class Factory(
        private val transport: NearbyTransport,
        private val deviceFallbackName: String,
        private val repo: GameRepository,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return NearbyViewModel(transport, deviceFallbackName, repo) as T
        }
    }
}
