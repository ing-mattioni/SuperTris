package it.claudio.supertris.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.claudio.supertris.ai.SuperTrisAi
import it.claudio.supertris.core.Difficulty
import it.claudio.supertris.core.GameMode
import it.claudio.supertris.core.GameState
import it.claudio.supertris.core.Move
import it.claudio.supertris.core.SuperTrisRules
import it.claudio.supertris.data.GameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class GameUiState(
    val difficulty: Difficulty = Difficulty.FACILE,
    val gameMode: GameMode = GameMode.VS_AI,
    val humanMark: Int = SuperTrisRules.X,
    val turnMark: Int = SuperTrisRules.X,
    val opponentName: String? = null,
    val isHumanTurn: Boolean = true,
    val isAiThinking: Boolean = false,
    val forcedMicro: Int = -1,
    val cells: IntArray = IntArray(81) { SuperTrisRules.EMPTY },
    val microStatus: IntArray = IntArray(9) { SuperTrisRules.STATUS_IN_CORSO },
    val macroStatus: Int = SuperTrisRules.STATUS_IN_CORSO,
    val isGameOver: Boolean = false,
    val lastResolvedMicro: Int = -1,
    val lastResolvedMark: Int = SuperTrisRules.EMPTY,
    val microCelebrationToken: Int = 0,
    val gameOverCelebrationToken: Int = 0,
    // NEARBY: rivincita chiesta localmente, in attesa dell'avversario.
    val waitingRematch: Boolean = false,
)

class GameSessionViewModel(
    private val repo: GameRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState

    private var aiJob: Job? = null
    private var currentState: GameState? = null
    private var isGameVisible: Boolean = false

    private val celebrations = CelebrationTracker()

    init {
        repo.gameStateFlow
            .onEach { st ->
                currentState = st
                if (st != null) {
                    pushUi(st, isAiThinking = _uiState.value.isAiThinking)
                }
            }
            .launchIn(viewModelScope)
    }

    fun setGameVisible(visible: Boolean) {
        if (isGameVisible == visible) return
        isGameVisible = visible

        if (!visible) {
            aiJob?.cancel()
            _uiState.value = _uiState.value.copy(isAiThinking = false)
            return
        }

        val st = currentState ?: return
        pushUi(st, isAiThinking = false)
        maybeTriggerAiMove(st)
    }

    fun startNewGame(difficulty: Difficulty, gameMode: GameMode = GameMode.VS_AI) {
        aiJob?.cancel()
        celebrations.reset()
        viewModelScope.launch {
            val st = SuperTrisRules.newGame(difficulty = difficulty, gameMode = gameMode, random = Random.Default)
            repo.saveGame(st)
            currentState = st
            pushUi(st, isAiThinking = false)
            maybeTriggerAiMove(st)
        }
    }

    // In PASS_AND_PLAY entrambi i giocatori toccano lo stesso schermo.
    private fun isLocalTurn(state: GameState): Boolean = when (state.gameMode) {
        GameMode.PASS_AND_PLAY -> true
        else -> SuperTrisRules.isHumanTurn(state)
    }

    fun onHumanTap(micro: Int, cell: Int) {
        val st = currentState ?: return
        if (st.isGameOver()) return
        if (!isLocalTurn(st)) return

        val move = Move(micro = micro, cell = cell)
        if (!SuperTrisRules.isLegalMove(st, move)) return

        viewModelScope.launch {
            val next = SuperTrisRules.applyMove(st, move)
            celebrations.register(previous = st, next = next, move = move)
            repo.saveGame(next)
            currentState = next
            pushUi(next, isAiThinking = false)
            maybeTriggerAiMove(next)
        }
    }

    private fun maybeTriggerAiMove(state: GameState) {
        if (state.gameMode != GameMode.VS_AI) return
        if (!isGameVisible) return
        if (state.isGameOver()) return
        if (SuperTrisRules.isHumanTurn(state)) return

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            pushUi(state, isAiThinking = true)

            val budget = when (state.difficulty) {
                Difficulty.FACILE -> 10L
                Difficulty.MEDIO -> 250L
                Difficulty.DIFFICILE -> 5_000L
            }.coerceAtMost(5_000L)

            val aiMove = withContext(Dispatchers.Default) {
                SuperTrisAi.chooseMove(state = state, timeBudgetMs = budget, active = { isActive })
            }

            val next = SuperTrisRules.applyMove(state, aiMove)
            celebrations.register(previous = state, next = next, move = aiMove)
            repo.saveGame(next)
            currentState = next
            pushUi(next, isAiThinking = false)
            maybeTriggerAiMove(next)
        }
    }

    private fun pushUi(state: GameState, isAiThinking: Boolean) {
        _uiState.value = GameUiState(
            difficulty = state.difficulty,
            gameMode = state.gameMode,
            humanMark = state.humanMark,
            turnMark = state.turn,
            isHumanTurn = isLocalTurn(state),
            isAiThinking = isAiThinking,
            forcedMicro = state.forcedMicro,
            cells = state.cells,
            microStatus = state.microStatus,
            macroStatus = state.macroStatus,
            isGameOver = state.isGameOver(),
            lastResolvedMicro = celebrations.lastResolvedMicro,
            lastResolvedMark = celebrations.lastResolvedMark,
            microCelebrationToken = celebrations.microCelebrationToken,
            gameOverCelebrationToken = celebrations.gameOverCelebrationToken,
        )
    }

    class Factory(
        private val repo: GameRepository,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return GameSessionViewModel(repo) as T
        }
    }
}