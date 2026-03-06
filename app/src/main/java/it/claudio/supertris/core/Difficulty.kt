package it.claudio.supertris.core

import kotlinx.serialization.Serializable

@Serializable
enum class Difficulty {
    FACILE,
    MEDIO,
    DIFFICILE,
}