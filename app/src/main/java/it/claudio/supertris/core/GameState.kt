package it.claudio.supertris.core

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val difficulty: Difficulty,
    // Default per retrocompatibilita' con le partite salvate prima del multiplayer.
    val gameMode: GameMode = GameMode.VS_AI,
    // In VS_AI e NEARBY e' il simbolo del giocatore locale; in PASS_AND_PLAY non e' significativo.
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