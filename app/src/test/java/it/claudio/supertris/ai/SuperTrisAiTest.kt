package it.claudio.supertris.ai

import it.claudio.supertris.core.Difficulty
import it.claudio.supertris.core.GameMode
import it.claudio.supertris.core.GameState
import it.claudio.supertris.core.Move
import it.claudio.supertris.core.SuperTrisRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Regressione v1.2.3: l'MCTS del livello Difficile valutava ogni nodo dal punto
 * di vista dell'IA, anche quelli in cui muove l'avversario. La selezione UCT
 * supponeva quindi che l'umano scegliesse la risposta migliore per l'IA, e
 * l'IA non vedeva le vittorie immediate che lasciava all'avversario.
 */
class SuperTrisAiTest {

    // Tabellone visto come griglia grande 9x9: ogni gruppo di 3 caratteri e' una
    // riga di una micro. A = IA, U = umano, . = vuota. L'umano ha vinto le micro
    // 0 e 1 e minaccia la micro 2 (cella 2): se ci arriva vince la partita.
    // L'IA deve giocare nella micro 4: le celle 0 e 1 mandano l'umano in micro
    // chiuse (gioco libero), la cella 2 vince la micro 4 ma lo manda proprio
    // nella micro 2. L'unica mossa che non perde subito e' la cella 5.
    private val trappola = listOf(
        "UUU U.A UU.",
        "AA. .UA A..",
        "... ..U ..A",
        "... ... ...",
        "... UA. ...",
        "... AUU ...",
        "... ... ...",
        "... ... ...",
        "... ... ...",
    )

    @Test
    fun difficile_nonRegalaAllAvversarioUnaVittoriaImmediata() {
        val errori = mutableListOf<String>()
        for (ai in listOf(SuperTrisRules.X, SuperTrisRules.O)) {
            val st = statoTrappola(ai)
            assertEquals(
                "La posizione deve avere una sola mossa sicura",
                listOf(Move(micro = 4, cell = 5)),
                SuperTrisRules.legalMoves(st).filterNot { regalaVittoriaImmediata(st, it) },
            )

            repeat(SEMI) { seed ->
                // Budget a iterazioni invece che a tempo: risultato deterministico su qualsiasi macchina.
                var iterazioni = 0
                val mossa = SuperTrisAi.chooseMove(
                    state = st,
                    timeBudgetMs = 60_000,
                    random = Random(seed),
                    active = { iterazioni++ < ITERAZIONI },
                )
                if (regalaVittoriaImmediata(st, mossa)) errori += "ia=$ai seed=$seed $mossa"
            }
        }
        assertTrue("Mosse che regalano la vittoria all'avversario: $errori", errori.isEmpty())
    }

    // True se dopo [mossa] l'avversario ha una risposta che chiude subito la partita a suo favore.
    private fun regalaVittoriaImmediata(st: GameState, mossa: Move): Boolean {
        val dopo = SuperTrisRules.applyMove(st, mossa)
        val vittoriaAvversario = if (dopo.turn == SuperTrisRules.X) SuperTrisRules.STATUS_X else SuperTrisRules.STATUS_O
        return SuperTrisRules.legalMoves(dopo).any { SuperTrisRules.applyMove(dopo, it).macroStatus == vittoriaAvversario }
    }

    private fun statoTrappola(ai: Int): GameState {
        val umano = SuperTrisRules.other(ai)
        val cells = IntArray(81) { SuperTrisRules.EMPTY }
        trappola.map { it.replace(" ", "") }.forEachIndexed { r, riga ->
            riga.forEachIndexed { c, ch ->
                val micro = (r / 3) * 3 + c / 3
                val cella = (r % 3) * 3 + c % 3
                cells[micro * 9 + cella] = when (ch) {
                    'A' -> ai
                    'U' -> umano
                    else -> SuperTrisRules.EMPTY
                }
            }
        }
        val vintaDaUmano = if (umano == SuperTrisRules.X) SuperTrisRules.STATUS_X else SuperTrisRules.STATUS_O
        val microStatus = IntArray(9) { SuperTrisRules.STATUS_IN_CORSO }
        microStatus[0] = vintaDaUmano
        microStatus[1] = vintaDaUmano

        return GameState(
            difficulty = Difficulty.DIFFICILE,
            gameMode = GameMode.VS_AI,
            humanMark = umano,
            turn = ai,
            forcedMicro = 4,
            cells = cells,
            microStatus = microStatus,
            macroStatus = SuperTrisRules.STATUS_IN_CORSO,
            moveCount = cells.count { it != SuperTrisRules.EMPTY },
        )
    }

    private companion object {
        const val SEMI = 20
        const val ITERAZIONI = 3_000
    }
}
