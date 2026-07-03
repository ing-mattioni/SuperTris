package it.claudio.supertris.net

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetMessageTest {

    @Test
    fun encodeDecode_roundTrip_perTuttiIMessaggiSemplici() {
        val messaggi = listOf(
            NetMessage.Hello(PROTOCOL_VERSION, "Pixel di Claudio"),
            NetMessage.GameStart(guestMark = 2, firstTurn = 1),
            NetMessage.MoveMsg(moveIndex = 12, micro = 4, cell = 8),
            NetMessage.SyncRequest,
            NetMessage.RematchRequest,
        )
        for (msg in messaggi) {
            val decoded = NetCodec.decodeOrNull(NetCodec.encode(msg))
            assertEquals(msg, decoded)
        }
    }

    @Test
    fun encodeDecode_roundTrip_stateSync() {
        val originale = NetMessage.StateSync(
            hostMark = 1,
            turn = 2,
            forcedMicro = 5,
            cells = IntArray(81) { it % 3 },
            microStatus = IntArray(9) { it % 4 },
            macroStatus = 0,
            moveCount = 27,
        )

        val decoded = NetCodec.decodeOrNull(NetCodec.encode(originale)) as NetMessage.StateSync

        assertEquals(originale.hostMark, decoded.hostMark)
        assertEquals(originale.turn, decoded.turn)
        assertEquals(originale.forcedMicro, decoded.forcedMicro)
        assertArrayEquals(originale.cells, decoded.cells)
        assertArrayEquals(originale.microStatus, decoded.microStatus)
        assertEquals(originale.macroStatus, decoded.macroStatus)
        assertEquals(originale.moveCount, decoded.moveCount)
    }

    @Test
    fun decodeOrNull_restituisceNull_suDatiNonValidi() {
        assertNull(NetCodec.decodeOrNull("non-e-json".toByteArray()))
        assertNull(NetCodec.decodeOrNull(byteArrayOf(0, 1, 2)))
        assertNull(NetCodec.decodeOrNull("""{"type":"sconosciuto"}""".toByteArray()))
    }
}
