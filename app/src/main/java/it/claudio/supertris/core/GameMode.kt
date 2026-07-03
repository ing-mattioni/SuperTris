package it.claudio.supertris.core

import kotlinx.serialization.Serializable

@Serializable
enum class GameMode {
    VS_AI,
    PASS_AND_PLAY,
    NEARBY,
    ONLINE,
}

/** Modalita' con un avversario umano remoto (mosse scambiate, regole deterministiche). */
val GameMode.isRemote: Boolean
    get() = this == GameMode.NEARBY || this == GameMode.ONLINE
