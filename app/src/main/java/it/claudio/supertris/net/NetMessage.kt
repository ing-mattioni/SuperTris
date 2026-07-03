package it.claudio.supertris.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val PROTOCOL_VERSION = 1

/**
 * Protocollo della partita su due telefoni. Entrambi i dispositivi applicano
 * le stesse [it.claudio.supertris.core.SuperTrisRules] deterministiche: basta
 * scambiarsi le mosse. L'host e' l'autorita' in caso di disallineamento.
 */
@Serializable
sealed interface NetMessage {

    @Serializable
    @SerialName("hello")
    data class Hello(
        val protocolVersion: Int,
        val playerName: String,
    ) : NetMessage

    /** Inviato dall'host: assegna il simbolo del guest e chi inizia. */
    @Serializable
    @SerialName("game_start")
    data class GameStart(
        val guestMark: Int,
        val firstTurn: Int,
    ) : NetMessage

    /** [moveIndex] = moveCount dello stato PRIMA della mossa, per rilevare disallineamenti. */
    @Serializable
    @SerialName("move")
    data class MoveMsg(
        val moveIndex: Int,
        val micro: Int,
        val cell: Int,
    ) : NetMessage

    /** Il guest chiede all'host lo stato autoritativo. */
    @Serializable
    @SerialName("sync_request")
    data object SyncRequest : NetMessage

    /** Stato autoritativo dell'host, neutro rispetto al dispositivo (niente humanMark). */
    @Serializable
    @SerialName("state_sync")
    data class StateSync(
        val hostMark: Int,
        val turn: Int,
        val forcedMicro: Int,
        val cells: IntArray,
        val microStatus: IntArray,
        val macroStatus: Int,
        val moveCount: Int,
    ) : NetMessage

    @Serializable
    @SerialName("rematch")
    data object RematchRequest : NetMessage
}

object NetCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(msg: NetMessage): ByteArray =
        json.encodeToString(NetMessage.serializer(), msg).encodeToByteArray()

    fun decodeOrNull(bytes: ByteArray): NetMessage? = runCatching {
        json.decodeFromString(NetMessage.serializer(), bytes.decodeToString())
    }.getOrNull()
}
