package it.claudio.supertris.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuperTrisRulesTest {

    @Test
    fun applyMove_updatesForcedMicro_toPlayedCell() {
        val st = SuperTrisRules.newGame(Difficulty.FACILE)
        val next = SuperTrisRules.applyMove(
            st.copy(turn = SuperTrisRules.X, forcedMicro = -1),
            Move(micro = 0, cell = 8),
        )
        assertEquals(8, next.forcedMicro)
    }

    @Test
    fun applyMove_forcedMicroBecomesAny_whenTargetMicroIsClosed() {
        val base = SuperTrisRules.newGame(Difficulty.FACILE)
        val microStatus = base.microStatus.clone()
        microStatus[8] = SuperTrisRules.STATUS_X // micro 8 chiusa

        val st = base.copy(
            turn = SuperTrisRules.X,
            forcedMicro = -1,
            microStatus = microStatus,
        )

        val next = SuperTrisRules.applyMove(st, Move(micro = 0, cell = 8))
        assertEquals(-1, next.forcedMicro)
    }

    @Test
    fun applyMove_winsMicro_whenCompletingLine() {
        val base = SuperTrisRules.newGame(Difficulty.FACILE)
        val cells = base.cells.clone()
        // micro 0: X X _
        cells[0] = SuperTrisRules.X
        cells[1] = SuperTrisRules.X

        val st = base.copy(
            turn = SuperTrisRules.X,
            forcedMicro = -1,
            cells = cells,
        )

        val next = SuperTrisRules.applyMove(st, Move(micro = 0, cell = 2))
        assertEquals(SuperTrisRules.STATUS_X, next.microStatus[0])
    }

    @Test
    fun applyMove_winsMacro_whenCompletingThreeMicrosInRow() {
        val base = SuperTrisRules.newGame(Difficulty.FACILE)

        val microStatus = base.microStatus.clone()
        microStatus[0] = SuperTrisRules.STATUS_X
        microStatus[1] = SuperTrisRules.STATUS_X
        microStatus[2] = SuperTrisRules.STATUS_IN_CORSO

        val cells = base.cells.clone()
        // micro 2: X X _
        val micro2Base = 2 * 9
        cells[micro2Base + 0] = SuperTrisRules.X
        cells[micro2Base + 1] = SuperTrisRules.X

        val st = base.copy(
            turn = SuperTrisRules.X,
            forcedMicro = -1,
            cells = cells,
            microStatus = microStatus,
        )

        val next = SuperTrisRules.applyMove(st, Move(micro = 2, cell = 2))
        assertEquals(SuperTrisRules.STATUS_X, next.microStatus[2])
        assertEquals(SuperTrisRules.STATUS_X, next.macroStatus)
        assertTrue(next.isGameOver())
    }

    @Test
    fun legalMoves_respectsForcedMicro() {
        val base = SuperTrisRules.newGame(Difficulty.FACILE)
        val st = base.copy(forcedMicro = 3)
        val moves = SuperTrisRules.legalMoves(st)
        assertTrue(moves.isNotEmpty())
        assertTrue(moves.all { it.micro == 3 })
    }
}