package it.claudio.supertris.core

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val difficulty: Difficulty,
    val humanMark: Int,
    val turn: Int,
    val forcedMicro: Int, // -1 = puoi giocare ovunque
    val cells: IntArray, // 81 celle: micro(0..8)*9 + cella(0..8)
    val microStatus: IntArray, // 9 micro: 0 in corso, 1 X, 2 O, 3 pareggio
    val macroStatus: Int, // 0 in corso, 1 X, 2 O, 3 pareggio
    val moveCount: Int,
) {
    fun isGameOver(): Boolean = macroStatus != SuperTrisRules.STATUS_IN_CORSO
}