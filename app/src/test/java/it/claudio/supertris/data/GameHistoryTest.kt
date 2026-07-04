package it.claudio.supertris.data

import it.claudio.supertris.core.Difficulty
import it.claudio.supertris.core.GameMode
import it.claudio.supertris.core.SuperTrisRules
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameHistoryTest {

    private fun playRandomGame(seed: Int) = run {
        val random = Random(seed)
        var st = SuperTrisRules.newRemoteGame(
            localMark = SuperTrisRules.X,
            firstTurn = SuperTrisRules.O,
            gameMode = GameMode.NEARBY,
        )
        while (!st.isGameOver()) {
            val moves = SuperTrisRules.legalMoves(st)
            st = SuperTrisRules.applyMove(st, moves[random.nextInt(moves.size)])
        }
        st
    }

    @Test
    fun moveHistory_registraTutteLeMosseInOrdine() {
        val finale = playRandomGame(11)
        assertEquals(finale.moveCount, finale.moveHistory.size)
        assertTrue(finale.moveHistory.all { it in 0 until 81 })
    }

    @Test
    fun buildHistoryEntry_derivaPrimoTurnoEdEsito() {
        val finale = playRandomGame(23)
        val entry = buildHistoryEntry(finale, opponentName = "Marco", timestampMs = 123L)

        assertNotNull(entry)
        entry!!
        assertEquals(SuperTrisRules.O, entry.firstTurn) // il gioco e' partito con O
        assertEquals(finale.moveHistory, entry.moves)
        assertEquals("Marco", entry.opponentName)
        assertEquals(123L, entry.timestampMs)

        val expectedWinner = when (finale.macroStatus) {
            SuperTrisRules.STATUS_X -> SuperTrisRules.X
            SuperTrisRules.STATUS_O -> SuperTrisRules.O
            else -> SuperTrisRules.EMPTY
        }
        assertEquals(expectedWinner, entry.winnerMark)
    }

    @Test
    fun buildHistoryEntry_nullSePartitaInCorso() {
        val inCorso = SuperTrisRules.newGame(Difficulty.FACILE)
        assertNull(buildHistoryEntry(inCorso, opponentName = null))
    }

    @Test
    fun rebuildReplayState_ricostruisceOgniPosizioneFinoAlFinale() {
        val finale = playRandomGame(37)
        val entry = buildHistoryEntry(finale, opponentName = null)!!

        // Posizione 0: tabellone vuoto, tocca al primo giocatore.
        val inizio = rebuildReplayState(entry, 0)
        assertEquals(0, inizio.moveCount)
        assertEquals(entry.firstTurn, inizio.turn)

        // Posizione finale: identica allo stato reale.
        val ricostruito = rebuildReplayState(entry, entry.moves.size)
        assertArrayEquals(finale.cells, ricostruito.cells)
        assertArrayEquals(finale.microStatus, ricostruito.microStatus)
        assertEquals(finale.macroStatus, ricostruito.macroStatus)
        assertEquals(finale.moveCount, ricostruito.moveCount)

        // Ogni posizione intermedia e' coerente.
        for (k in 1 until entry.moves.size) {
            assertEquals(k, rebuildReplayState(entry, k).moveCount)
        }
    }

    @Test
    fun gameStats_applying_aggiornaLaSezioneGiusta() {
        val finale = playRandomGame(41)
        val entryNearby = buildHistoryEntry(finale, opponentName = "Marco")!!

        var stats = GameStats().applying(entryNearby)
        assertEquals(1, stats.nearby.played)
        assertEquals(0, stats.online.played)

        // VS_AI difficile, vittoria locale simulata.
        val entryAi = entryNearby.copy(
            gameMode = GameMode.VS_AI,
            difficulty = Difficulty.DIFFICILE,
            localMark = entryNearby.winnerMark.takeIf { it != SuperTrisRules.EMPTY } ?: SuperTrisRules.X,
        )
        stats = stats.applying(entryAi)
        assertEquals(1, stats.aiDifficile.played)
        if (entryAi.winnerMark != SuperTrisRules.EMPTY) {
            assertEquals(1, stats.aiDifficile.wins)
        }

        // Pass-and-play: conta per simbolo.
        val entryPass = entryNearby.copy(gameMode = GameMode.PASS_AND_PLAY)
        stats = stats.applying(entryPass)
        assertEquals(1, stats.passAndPlay.played)

        assertEquals(3, stats.totalPlayed)
    }
}
