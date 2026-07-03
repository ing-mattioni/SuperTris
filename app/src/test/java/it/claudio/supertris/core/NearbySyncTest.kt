package it.claudio.supertris.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Il multiplayer NEARBY scambia solo le mosse: i due telefoni devono restare
 * perfettamente sincronizzati applicando le stesse regole deterministiche.
 */
class NearbySyncTest {

    @Test
    fun dueDispositivi_cheApplicanoLeStesseMosse_restanoSincronizzati() {
        var host = SuperTrisRules.newRemoteGame(localMark = SuperTrisRules.X, firstTurn = SuperTrisRules.X)
        var guest = SuperTrisRules.newRemoteGame(localMark = SuperTrisRules.O, firstTurn = SuperTrisRules.X)
        val random = Random(42)

        while (!host.isGameOver()) {
            val moves = SuperTrisRules.legalMoves(host)
            assertTrue(moves.isNotEmpty())
            val move = moves[random.nextInt(moves.size)]

            host = SuperTrisRules.applyMove(host, move)
            guest = SuperTrisRules.applyMove(guest, move)

            assertArrayEquals(host.cells, guest.cells)
            assertArrayEquals(host.microStatus, guest.microStatus)
            assertEquals(host.turn, guest.turn)
            assertEquals(host.forcedMicro, guest.forcedMicro)
            assertEquals(host.macroStatus, guest.macroStatus)
            assertEquals(host.moveCount, guest.moveCount)
        }

        assertTrue(guest.isGameOver())
        assertEquals(GameMode.NEARBY, host.gameMode)
    }

    @Test
    fun statoSalvatoPrimaDelMultiplayer_siDecodificaComeVsAi() {
        // JSON senza il campo gameMode, come i salvataggi della v1.0.
        val legacyJson = """
            {"difficulty":"MEDIO","humanMark":1,"turn":2,"forcedMicro":-1,
             "cells":[${IntArray(81).joinToString(",")}],
             "microStatus":[0,0,0,0,0,0,0,0,0],"macroStatus":0,"moveCount":0}
        """.trimIndent()

        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val stato = json.decodeFromString(GameState.serializer(), legacyJson)

        assertEquals(GameMode.VS_AI, stato.gameMode)
        assertEquals(Difficulty.MEDIO, stato.difficulty)
    }
}
