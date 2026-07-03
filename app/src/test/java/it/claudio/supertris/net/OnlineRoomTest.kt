package it.claudio.supertris.net

import it.claudio.supertris.core.GameMode
import it.claudio.supertris.core.SuperTrisRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class OnlineRoomTest {

    private fun roomWithMoves(moves: List<RoomMove>) = OnlineRoom(
        code = "TEST1",
        status = "playing",
        hostUid = "uid-host",
        guestUid = "uid-guest",
        hostName = "Claudio",
        guestName = "Beta",
        hostMark = SuperTrisRules.X,
        firstTurn = SuperTrisRules.X,
        round = 0,
        hostRematch = false,
        guestRematch = false,
        moves = moves,
        hostSeenMs = null,
        guestSeenMs = null,
        protocolVersion = ONLINE_PROTOCOL_VERSION,
    )

    @Test
    fun buildLocalState_ricostruisceLoStessoStatoSuEntrambiILati() {
        // Simula una partita di mosse casuali legali e verifica che host e
        // guest ricostruiscano lo stesso tabellone dalla lista mosse.
        val random = Random(7)
        var reference = SuperTrisRules.newRemoteGame(
            localMark = SuperTrisRules.X,
            firstTurn = SuperTrisRules.X,
            gameMode = GameMode.ONLINE,
        )
        val moves = mutableListOf<RoomMove>()
        var index = 0
        while (!reference.isGameOver()) {
            val legal = SuperTrisRules.legalMoves(reference)
            val m = legal[random.nextInt(legal.size)]
            reference = SuperTrisRules.applyMove(reference, m)
            moves.add(RoomMove(index = index, micro = m.micro, cell = m.cell))
            index++
        }

        val room = roomWithMoves(moves)
        val hostState = room.buildLocalState(localIsHost = true)!!
        val guestState = room.buildLocalState(localIsHost = false)!!

        assertEquals(reference.macroStatus, hostState.macroStatus)
        assertEquals(reference.moveCount, hostState.moveCount)
        assertEquals(hostState.macroStatus, guestState.macroStatus)
        assertEquals(hostState.turn, guestState.turn)
        // Prospettive opposte: stesso tabellone, simbolo locale diverso.
        assertEquals(SuperTrisRules.X, hostState.humanMark)
        assertEquals(SuperTrisRules.O, guestState.humanMark)
        assertNotEquals(hostState.humanMark, guestState.humanMark)
        assertEquals(GameMode.ONLINE, hostState.gameMode)
    }

    @Test
    fun buildLocalState_restituisceNull_conMossaIllegale() {
        // Due mosse nella stessa cella: la seconda e' illegale.
        val room = roomWithMoves(
            listOf(
                RoomMove(index = 0, micro = 4, cell = 4),
                RoomMove(index = 1, micro = 4, cell = 4),
            ),
        )
        assertNull(room.buildLocalState(localIsHost = true))
    }

    @Test
    fun buildLocalState_restituisceNull_conIndiciNonProgressivi() {
        val room = roomWithMoves(
            listOf(
                RoomMove(index = 0, micro = 4, cell = 4),
                RoomMove(index = 5, micro = 4, cell = 0),
            ),
        )
        assertNull(room.buildLocalState(localIsHost = true))
    }

    @Test
    fun generateRoomCode_formatoValido() {
        val random = Random(3)
        repeat(50) {
            val code = OnlineRoomClient.generateRoomCode(random)
            assertEquals(OnlineRoomClient.CODE_LENGTH, code.length)
            assertTrue(OnlineRoomClient.isValidCodeFormat(code))
            // Niente caratteri ambigui.
            assertTrue(code.none { it in "0O1IL" })
        }
    }

    @Test
    fun isValidCodeFormat_normalizzaMinuscoleESpazi() {
        assertTrue(OnlineRoomClient.isValidCodeFormat("ABCDE"))
        assertTrue(OnlineRoomClient.isValidCodeFormat("abcde"))
        assertTrue(OnlineRoomClient.isValidCodeFormat(" abcde "))
        assertTrue(!OnlineRoomClient.isValidCodeFormat("AB"))
        assertTrue(!OnlineRoomClient.isValidCodeFormat("ABCD0"))
    }
}
