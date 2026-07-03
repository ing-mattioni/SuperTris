package it.claudio.supertris.core

import kotlinx.serialization.Serializable

@Serializable
enum class GameMode {
    VS_AI,
    PASS_AND_PLAY,
    NEARBY,
}
