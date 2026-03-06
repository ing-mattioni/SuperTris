package it.claudio.supertris.core

import kotlinx.serialization.Serializable

@Serializable
data class Move(
    val micro: Int,
    val cell: Int,
)