package it.claudio.supertris.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.claudio.supertris.ai.SuperTrisAi
import it.claudio.supertris.core.Difficulty
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class GameUiState(
    val difficulty: Difficulty = Difficulty.FACILE,
    val humanMark: Int = SuperTrisRules.X,
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
)

class GameSessionViewModel(
    private val repo: GameRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState

    private var aiJob: Job? = null
    private var currentState: GameState? = null
    private var isGameVisible: Boolean = false

    private var lastResolvedMicro: Int = -1
    private var lastResolvedMark: Int = SuperTrisRules.EMPTY
    private var microCelebrationToken: Int = 0
    private var gameOverCelebrationToken: Int = 0

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

    fun startNewGame(difficulty: Difficulty) {
        aiJob?.cancel()
        resetCelebrations()
        viewModelScope.launch {
            val st = SuperTrisRules.newGame(difficulty = difficulty, random = Random.Default)
            repo.saveGame(st)
            currentState = st
            pushUi(st, isAiThinking = false)
            maybeTriggerAiMove(st)
        }
    }

    fun onHumanTap(micro: Int, cell: Int) {
        val st = currentState ?: return
        if (st.isGameOver()) return
        if (!SuperTrisRules.isHumanTurn(st)) return

        val move = Move(micro = micro, cell = cell)
        if (!SuperTrisRules.isLegalMove(st, move)) return

        viewModelScope.launch {
            val next = SuperTrisRules.applyMove(st, move)
            registerCelebrations(previous = st, next = next, move = move)
            repo.saveGame(next)
            currentState = next
            pushUi(next, isAiThinking = false)
            maybeTriggerAiMove(next)
        }
    }

    private fun maybeTriggerAiMove(state: GameState) {
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
                SuperTrisAi.chooseMove(state = state, timeBudgetMs = budget)
            }

            val next = SuperTrisRules.applyMove(state, aiMove)
            registerCelebrations(previous = state, next = next, move = aiMove)
            repo.saveGame(next)
            currentState = next
            pushUi(next, isAiThinking = false)
            maybeTriggerAiMove(next)
        }
    }

    private fun registerCelebrations(previous: GameState, next: GameState, move: Move) {
        lastResolvedMicro = -1
        lastResolvedMark = SuperTrisRules.EMPTY

        val prevMicro = previous.microStatus[move.micro]
        val nextMicro = next.microStatus[move.micro]
        if (prevMicro == SuperTrisRules.STATUS_IN_CORSO) {
            when (nextMicro) {
                SuperTrisRules.STATUS_X -> {
                    lastResolvedMicro = move.micro
                    lastResolvedMark = SuperTrisRules.X
                    microCelebrationToken++
                }
                SuperTrisRules.STATUS_O -> {
                    lastResolvedMicro = move.micro
                    lastResolvedMark = SuperTrisRules.O
                    microCelebrationToken++
                }
            }
        }

        if (!previous.isGameOver() && next.isGameOver()) {
            gameOverCelebrationToken++
        }
    }

    private fun resetCelebrations() {
        lastResolvedMicro = -1
        lastResolvedMark = SuperTrisRules.EMPTY
        microCelebrationToken = 0
        gameOverCelebrationToken = 0
    }

    private fun pushUi(state: GameState, isAiThinking: Boolean) {
        _uiState.value = GameUiState(
            difficulty = state.difficulty,
            humanMark = state.humanMark,
            isHumanTurn = SuperTrisRules.isHumanTurn(state),
            isAiThinking = isAiThinking,
            forcedMicro = state.forcedMicro,
            cells = state.cells,
            microStatus = state.microStatus,
            macroStatus = state.macroStatus,
            isGameOver = state.isGameOver(),
            lastResolvedMicro = lastResolvedMicro,
            lastResolvedMark = lastResolvedMark,
            microCelebrationToken = microCelebrationToken,
            gameOverCelebrationToken = gameOverCelebrationToken,
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