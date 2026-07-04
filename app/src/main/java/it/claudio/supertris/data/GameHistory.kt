package it.claudio.supertris.data

import it.claudio.supertris.core.Difficulty
import it.claudio.supertris.core.GameMode
import it.claudio.supertris.core.GameState
import it.claudio.supertris.core.Move
import it.claudio.supertris.core.SuperTrisRules
import kotlinx.serialization.Serializable

// ---------- Statistiche ----------

@Serializable
data class ModeStats(
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
) {
    val played: Int get() = wins + losses + draws
}

/** Pass-and-play non ha un "giocatore locale": si contano le vittorie di X e O. */
@Serializable
data class PassPlayStats(
    val xWins: Int = 0,
    val oWins: Int = 0,
    val draws: Int = 0,
) {
    val played: Int get() = xWins + oWins + draws
}

@Serializable
data class GameStats(
    val aiFacile: ModeStats = ModeStats(),
    val aiMedio: ModeStats = ModeStats(),
    val aiDifficile: ModeStats = ModeStats(),
    val passAndPlay: PassPlayStats = PassPlayStats(),
    val nearby: ModeStats = ModeStats(),
    val online: ModeStats = ModeStats(),
) {
    val totalPlayed: Int
        get() = aiFacile.played + aiMedio.played + aiDifficile.played +
            passAndPlay.played + nearby.played + online.played
}

// ---------- Cronologia ----------

@Serializable
data class HistoryEntry(
    val timestampMs: Long,
    val gameMode: GameMode,
    val difficulty: Difficulty? = null,
    val opponentName: String? = null,
    val localMark: Int,
    val firstTurn: Int,
    val winnerMark: Int, // SuperTrisRules.EMPTY = pareggio
    val moves: List<Int>, // micro*9+cella, in ordine di gioco
)

/** Null per pareggio o pass-and-play (nessuna prospettiva locale). */
val HistoryEntry.localWon: Boolean?
    get() = when {
        winnerMark == SuperTrisRules.EMPTY -> null
        gameMode == GameMode.PASS_AND_PLAY -> null
        else -> winnerMark == localMark
    }

/** Costruisce la voce di cronologia da uno stato finale. Null se la partita non e' finita. */
fun buildHistoryEntry(
    state: GameState,
    opponentName: String?,
    timestampMs: Long = System.currentTimeMillis(),
): HistoryEntry? {
    if (!state.isGameOver()) return null
    val winnerMark = when (state.macroStatus) {
        SuperTrisRules.STATUS_X -> SuperTrisRules.X
        SuperTrisRules.STATUS_O -> SuperTrisRules.O
        else -> SuperTrisRules.EMPTY
    }
    // A fine partita il turno e' gia' passato: chi ha iniziato si ricava dalla parita'.
    val firstTurn = if (state.moveCount % 2 == 0) state.turn else SuperTrisRules.other(state.turn)
    return HistoryEntry(
        timestampMs = timestampMs,
        gameMode = state.gameMode,
        difficulty = if (state.gameMode == GameMode.VS_AI) state.difficulty else null,
        opponentName = opponentName,
        localMark = state.humanMark,
        firstTurn = firstTurn,
        winnerMark = winnerMark,
        moves = state.moveHistory,
    )
}

/** Ricostruisce il tabellone dopo le prime [upToMoves] mosse, per il replay. */
fun rebuildReplayState(entry: HistoryEntry, upToMoves: Int): GameState {
    var st = GameState(
        difficulty = entry.difficulty ?: Difficulty.FACILE,
        gameMode = entry.gameMode,
        humanMark = entry.localMark,
        turn = entry.firstTurn,
        forcedMicro = -1,
        cells = IntArray(81) { SuperTrisRules.EMPTY },
        microStatus = IntArray(9) { SuperTrisRules.STATUS_IN_CORSO },
        macroStatus = SuperTrisRules.STATUS_IN_CORSO,
        moveCount = 0,
    )
    for (encoded in entry.moves.take(upToMoves.coerceIn(0, entry.moves.size))) {
        val move = Move(micro = encoded / 9, cell = encoded % 9)
        if (!SuperTrisRules.isLegalMove(st, move)) break
        st = SuperTrisRules.applyMove(st, move)
    }
    return st
}

/** Aggiorna le statistiche con l'esito di [entry] (funzione pura, testabile). */
fun GameStats.applying(entry: HistoryEntry): GameStats {
    fun ModeStats.record(won: Boolean?): ModeStats = when (won) {
        true -> copy(wins = wins + 1)
        false -> copy(losses = losses + 1)
        null -> copy(draws = draws + 1)
    }

    return when (entry.gameMode) {
        GameMode.VS_AI -> when (entry.difficulty) {
            Difficulty.MEDIO -> copy(aiMedio = aiMedio.record(entry.localWon))
            Difficulty.DIFFICILE -> copy(aiDifficile = aiDifficile.record(entry.localWon))
            else -> copy(aiFacile = aiFacile.record(entry.localWon))
        }

        GameMode.PASS_AND_PLAY -> copy(
            passAndPlay = when (entry.winnerMark) {
                SuperTrisRules.X -> passAndPlay.copy(xWins = passAndPlay.xWins + 1)
                SuperTrisRules.O -> passAndPlay.copy(oWins = passAndPlay.oWins + 1)
                else -> passAndPlay.copy(draws = passAndPlay.draws + 1)
            },
        )

        GameMode.NEARBY -> copy(nearby = nearby.record(entry.localWon))
        GameMode.ONLINE -> copy(online = online.record(entry.localWon))
    }
}
