package it.claudio.supertris.core

import kotlin.random.Random

object SuperTrisRules {
    const val EMPTY = 0
    const val X = 1
    const val O = 2

    const val STATUS_IN_CORSO = 0
    const val STATUS_X = 1
    const val STATUS_O = 2
    const val STATUS_PAREGGIO = 3

    private val winLines = arrayOf(
        intArrayOf(0, 1, 2),
        intArrayOf(3, 4, 5),
        intArrayOf(6, 7, 8),
        intArrayOf(0, 3, 6),
        intArrayOf(1, 4, 7),
        intArrayOf(2, 5, 8),
        intArrayOf(0, 4, 8),
        intArrayOf(2, 4, 6),
    )

    fun other(mark: Int): Int = when (mark) {
        X -> O
        O -> X
        else -> error("Mark non valido: $mark")
    }

    fun newGame(
        difficulty: Difficulty,
        gameMode: GameMode = GameMode.VS_AI,
        random: Random = Random.Default,
    ): GameState {
        val humanMark = if (random.nextBoolean()) X else O
        val firstTurnIsHuman = random.nextBoolean()
        val firstTurn = if (firstTurnIsHuman) humanMark else other(humanMark)

        return GameState(
            difficulty = difficulty,
            gameMode = gameMode,
            humanMark = humanMark,
            turn = firstTurn,
            forcedMicro = -1,
            cells = IntArray(81) { EMPTY },
            microStatus = IntArray(9) { STATUS_IN_CORSO },
            macroStatus = STATUS_IN_CORSO,
            moveCount = 0,
        )
    }

    // Partita NEARBY: simboli e primo turno decisi dall'host, uguali su entrambi i telefoni.
    fun newNearbyGame(localMark: Int, firstTurn: Int): GameState {
        require(localMark == X || localMark == O) { "Mark non valido: $localMark" }
        require(firstTurn == X || firstTurn == O) { "Turno non valido: $firstTurn" }
        return GameState(
            difficulty = Difficulty.FACILE, // non usata: nessuna AI in questa modalita'
            gameMode = GameMode.NEARBY,
            humanMark = localMark,
            turn = firstTurn,
            forcedMicro = -1,
            cells = IntArray(81) { EMPTY },
            microStatus = IntArray(9) { STATUS_IN_CORSO },
            macroStatus = STATUS_IN_CORSO,
            moveCount = 0,
        )
    }

    fun isHumanTurn(state: GameState): Boolean = state.turn == state.humanMark
    fun aiMark(state: GameState): Int = other(state.humanMark)

    fun isLegalMove(state: GameState, move: Move): Boolean {
        if (state.isGameOver()) return false
        if (move.micro !in 0..8) return false
        if (move.cell !in 0..8) return false
        if (state.microStatus[move.micro] != STATUS_IN_CORSO) return false

        val forced = state.forcedMicro
        if (forced != -1 && state.microStatus[forced] == STATUS_IN_CORSO && move.micro != forced) return false

        val idx = move.micro * 9 + move.cell
        if (state.cells[idx] != EMPTY) return false
        return true
    }

    fun legalMoves(state: GameState): List<Move> {
        if (state.isGameOver()) return emptyList()

        val microsToScan: IntArray = if (state.forcedMicro != -1) {
            intArrayOf(state.forcedMicro)
        } else {
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
        }

        val moves = ArrayList<Move>(81)
        for (m in microsToScan) {
            if (state.microStatus[m] != STATUS_IN_CORSO) continue
            val base = m * 9
            for (c in 0..8) {
                if (state.cells[base + c] == EMPTY) {
                    moves.add(Move(micro = m, cell = c))
                }
            }
        }

        if (moves.isNotEmpty()) return moves

        // Se forcedMicro non ha mosse (micro chiusa), allora puoi giocare ovunque.
        if (state.forcedMicro != -1) {
            return legalMoves(state.copy(forcedMicro = -1))
        }
        return emptyList()
    }

    fun applyMove(state: GameState, move: Move): GameState {
        require(isLegalMove(state, move)) { "Mossa non valida: $move" }

        val newCells = state.cells.clone()
        val newMicroStatus = state.microStatus.clone()

        val idx = move.micro * 9 + move.cell
        newCells[idx] = state.turn

        // Aggiorna micro dove si e' giocato.
        newMicroStatus[move.micro] = computeMicroStatus(newCells, move.micro, newMicroStatus[move.micro])

        val newMacroStatus = computeMacroStatus(newMicroStatus)

        // Prossima micro obbligata = la cella giocata (0..8) -> micro (0..8).
        val nextForcedCandidate = move.cell
        val nextForced = if (newMicroStatus[nextForcedCandidate] == STATUS_IN_CORSO) nextForcedCandidate else -1

        return state.copy(
            turn = other(state.turn),
            forcedMicro = nextForced,
            cells = newCells,
            microStatus = newMicroStatus,
            macroStatus = newMacroStatus,
            moveCount = state.moveCount + 1,
        )
    }

    private fun computeMicroStatus(cells: IntArray, micro: Int, current: Int): Int {
        if (current != STATUS_IN_CORSO) return current
        val base = micro * 9

        for (line in winLines) {
            val a = cells[base + line[0]]
            if (a == EMPTY) continue
            val b = cells[base + line[1]]
            val c = cells[base + line[2]]
            if (a == b && b == c) {
                return if (a == X) STATUS_X else STATUS_O
            }
        }

        // Pareggio micro se piena.
        for (i in 0..8) {
            if (cells[base + i] == EMPTY) return STATUS_IN_CORSO
        }
        return STATUS_PAREGGIO
    }

    private fun computeMacroStatus(microStatus: IntArray): Int {
        // Trasforma microStatus in una griglia 3x3 di "mark vincente" (EMPTY se non vinta).
        val macro = IntArray(9) { i ->
            when (microStatus[i]) {
                STATUS_X -> X
                STATUS_O -> O
                else -> EMPTY
            }
        }

        for (line in winLines) {
            val a = macro[line[0]]
            if (a == EMPTY) continue
            val b = macro[line[1]]
            val c = macro[line[2]]
            if (a == b && b == c) {
                return if (a == X) STATUS_X else STATUS_O
            }
        }

        val allClosed = microStatus.all { it != STATUS_IN_CORSO }
        return if (allClosed) STATUS_PAREGGIO else STATUS_IN_CORSO
    }
}