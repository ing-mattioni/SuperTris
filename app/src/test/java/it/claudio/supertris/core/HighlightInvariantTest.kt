package it.claudio.supertris.core

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

/**
 * L'evidenziazione delle griglie in cui si puo' giocare (GameScreen) usa:
 *   highlighted = (forcedMicro == -1 && micro aperta) || forcedMicro == micro
 * Questo test garantisce che, in qualsiasi stato raggiungibile, l'insieme
 * delle griglie evidenziate coincida ESATTAMENTE con quelle in cui esistono
 * mosse legali: mai una griglia obbligata chiusa, mai zero evidenziazioni
 * a partita in corso (bug segnalato in v1.2.0).
 */
class HighlightInvariantTest {

    private fun highlightedMicros(state: GameState): Set<Int> =
        (0..8).filter { micro ->
            (state.forcedMicro == -1 && state.microStatus[micro] == SuperTrisRules.STATUS_IN_CORSO) ||
                state.forcedMicro == micro
        }.toSet()

    @Test
    fun leGriglieEvidenziate_coincidonoSempreConQuelleGiocabili() {
        repeat(300) { seed ->
            val random = Random(seed)
            var st = SuperTrisRules.newGame(Difficulty.FACILE, random = random)

            while (!st.isGameOver()) {
                val legalByMicro = SuperTrisRules.legalMoves(st).map { it.micro }.toSet()
                val highlighted = highlightedMicros(st)

                assertEquals(
                    "Mismatch al seed=$seed, mossa=${st.moveCount}, forced=${st.forcedMicro}",
                    legalByMicro,
                    highlighted,
                )
                // Se la partita e' in corso ci sono sempre griglie giocabili evidenziate.
                assertEquals(false, highlighted.isEmpty())

                val moves = SuperTrisRules.legalMoves(st)
                st = SuperTrisRules.applyMove(st, moves[random.nextInt(moves.size)])
            }
        }
    }
}
