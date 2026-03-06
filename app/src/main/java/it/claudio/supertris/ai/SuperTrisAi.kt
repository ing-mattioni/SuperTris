package it.claudio.supertris.ai

import it.claudio.supertris.core.Difficulty
import it.claudio.supertris.core.GameState
import it.claudio.supertris.core.Move
import it.claudio.supertris.core.SuperTrisRules
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

object SuperTrisAi {

    fun chooseMove(
        state: GameState,
        timeBudgetMs: Long,
        random: Random = Random.Default,
    ): Move {
        val moves = SuperTrisRules.legalMoves(state)
        if (moves.isEmpty()) error("Nessuna mossa disponibile")

        return when (state.difficulty) {
            Difficulty.FACILE -> moves.random(random)
            Difficulty.MEDIO -> chooseHeuristicMove(state, moves, random)
            Difficulty.DIFFICILE -> chooseMctsMove(state, moves, timeBudgetMs, random)
        }
    }

    private fun chooseHeuristicMove(state: GameState, moves: List<Move>, random: Random): Move {
        val ai = SuperTrisRules.aiMark(state)
        val opp = SuperTrisRules.other(ai)

        // 1) Se posso vincere subito una micro, fallo.
        for (m in moves.shuffled(random)) {
            val next = SuperTrisRules.applyMove(state, m)
            val expected = if (ai == SuperTrisRules.X) SuperTrisRules.STATUS_X else SuperTrisRules.STATUS_O
            if (next.microStatus[m.micro] == expected) return m
        }

        // 2) Blocca una vittoria immediata dell'avversario nella micro obbligata (euristica semplice).
        val forced = state.forcedMicro
        if (forced != -1) {
            val blockingMoves = moves.filter { it.micro == forced }
            for (m in blockingMoves.shuffled(random)) {
                val idx = m.micro * 9 + m.cell
                val tmp = state.cells.clone()
                tmp[idx] = opp
                if (wouldWinMicro(tmp, m.micro, opp)) return m
            }
        }

        // 3) Score base + evita di mandare l'avversario in una micro "calda".
        var best = moves[0]
        var bestScore = Long.MIN_VALUE
        for (m in moves) {
            var score = 0L
            score += cellPosScore(m.cell).toLong()
            score += microPosScore(m.micro).toLong()

            val nextForced = m.cell
            if (state.microStatus[nextForced] == SuperTrisRules.STATUS_IN_CORSO) {
                val oppThreat = microThreatScore(state.cells, nextForced, opp)
                score -= oppThreat * 3L
            }

            val next = SuperTrisRules.applyMove(state, m)
            val ms = next.microStatus[m.micro]
            val expectedAi = if (ai == SuperTrisRules.X) SuperTrisRules.STATUS_X else SuperTrisRules.STATUS_O
            if (ms == expectedAi) score += 200
            if (ms == SuperTrisRules.STATUS_PAREGGIO) score += 10

            // Tie-break casuale.
            score = score * 10 + random.nextInt(0, 10)

            if (score > bestScore) {
                bestScore = score
                best = m
            }
        }
        return best
    }

    private fun chooseMctsMove(
        state: GameState,
        rootMoves: List<Move>,
        timeBudgetMs: Long,
        random: Random,
    ): Move {
        val ai = SuperTrisRules.aiMark(state)
        val endAt = System.currentTimeMillis() + timeBudgetMs.coerceAtLeast(150)

        val root = Node(
            state = state,
            parent = null,
            move = null,
        )

        while (System.currentTimeMillis() < endAt) {
            var node = root
            var simState = state

            // Selection
            while (node.untriedMoves.isEmpty() && node.children.isNotEmpty()) {
                node = node.selectChildUct()
                simState = SuperTrisRules.applyMove(simState, node.move!!)
            }

            // Expansion
            if (node.untriedMoves.isNotEmpty() && !simState.isGameOver()) {
                val m = node.untriedMoves.removeAt(random.nextInt(node.untriedMoves.size))
                simState = SuperTrisRules.applyMove(simState, m)
                node = node.addChild(m, simState)
            }

            // Simulation (playout casuale)
            var rolloutState = simState
            var safety = 0
            while (!rolloutState.isGameOver() && safety < 200) {
                val moves = SuperTrisRules.legalMoves(rolloutState)
                if (moves.isEmpty()) break
                val m = moves[random.nextInt(moves.size)]
                rolloutState = SuperTrisRules.applyMove(rolloutState, m)
                safety++
            }

            val result = when (rolloutState.macroStatus) {
                SuperTrisRules.STATUS_X -> if (ai == SuperTrisRules.X) 1.0 else 0.0
                SuperTrisRules.STATUS_O -> if (ai == SuperTrisRules.O) 1.0 else 0.0
                SuperTrisRules.STATUS_PAREGGIO -> 0.5
                else -> 0.5
            }

            // Backpropagation
            while (true) {
                node.visits++
                node.totalScore += result
                node = node.parent ?: break
            }
        }

        // Scegli la mossa con piu' visite.
        val bestChild = root.children.maxByOrNull { it.visits } ?: return rootMoves.random(random)
        return bestChild.move!!
    }

    private class Node(
        val state: GameState,
        val parent: Node?,
        val move: Move?,
    ) {
        val children: MutableList<Node> = ArrayList()
        val untriedMoves: MutableList<Move> = SuperTrisRules.legalMoves(state).toMutableList()
        var visits: Int = 0
        var totalScore: Double = 0.0

        fun addChild(move: Move, newState: GameState): Node {
            val n = Node(
                state = newState,
                parent = this,
                move = move,
            )
            children.add(n)
            untriedMoves.remove(move)
            return n
        }

        fun selectChildUct(): Node {
            val c = 1.41421356237
            val logParent = ln((visits + 1).toDouble())
            return children.maxBy { child ->
                val exploit = if (child.visits == 0) 0.0 else child.totalScore / child.visits.toDouble()
                val explore = c * sqrt(logParent / (child.visits + 1e-9))
                exploit + explore
            }
        }
    }

    private fun cellPosScore(cell: Int): Int = when (cell) {
        4 -> 6
        0, 2, 6, 8 -> 4
        else -> 2
    }

    private fun microPosScore(micro: Int): Int = when (micro) {
        4 -> 3
        0, 2, 6, 8 -> 2
        else -> 1
    }

    private fun wouldWinMicro(cells: IntArray, micro: Int, mark: Int): Boolean {
        val base = micro * 9
        val lines = arrayOf(
            intArrayOf(0, 1, 2),
            intArrayOf(3, 4, 5),
            intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6),
            intArrayOf(1, 4, 7),
            intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8),
            intArrayOf(2, 4, 6),
        )
        for (line in lines) {
            val a = cells[base + line[0]]
            val b = cells[base + line[1]]
            val c = cells[base + line[2]]
            if (a == mark && b == mark && c == mark) return true
        }
        return false
    }

    // Quante linee hanno gia' 2 mark e 1 vuoto (minaccia immediata).
    private fun microThreatScore(cells: IntArray, micro: Int, mark: Int): Int {
        val base = micro * 9
        val lines = arrayOf(
            intArrayOf(0, 1, 2),
            intArrayOf(3, 4, 5),
            intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6),
            intArrayOf(1, 4, 7),
            intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8),
            intArrayOf(2, 4, 6),
        )
        var threats = 0
        for (line in lines) {
            var marks = 0
            var empties = 0
            for (i in line) {
                val v = cells[base + i]
                if (v == mark) marks++
                if (v == SuperTrisRules.EMPTY) empties++
            }
            if (marks == 2 && empties == 1) threats++
        }
        return threats
    }
}