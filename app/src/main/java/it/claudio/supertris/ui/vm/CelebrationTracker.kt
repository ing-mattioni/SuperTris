package it.claudio.supertris.ui.vm

import it.claudio.supertris.core.GameState
import it.claudio.supertris.core.Move
import it.claudio.supertris.core.SuperTrisRules

// Traccia le animazioni (conquista micro, fine partita) tra una mossa e la successiva.
class CelebrationTracker {
    var lastResolvedMicro: Int = -1
        private set
    var lastResolvedMark: Int = SuperTrisRules.EMPTY
        private set
    var microCelebrationToken: Int = 0
        private set
    var gameOverCelebrationToken: Int = 0
        private set

    fun register(previous: GameState, next: GameState, move: Move) {
        lastResolvedMicro = -1
        lastResolvedMark = SuperTrisRules.EMPTY

        val prevMicro = previous.microStatus[move.micro]
        val nextMicro = next.microStatus[move.micro]
        if (prevMicro == SuperTrisRules.STATUS_IN_CORSO &&
            (nextMicro == SuperTrisRules.STATUS_X || nextMicro == SuperTrisRules.STATUS_O)
        ) {
            lastResolvedMicro = move.micro
            lastResolvedMark = if (nextMicro == SuperTrisRules.STATUS_X) SuperTrisRules.X else SuperTrisRules.O
            microCelebrationToken++
        }

        if (!previous.isGameOver() && next.isGameOver()) {
            gameOverCelebrationToken++
        }
    }

    fun reset() {
        lastResolvedMicro = -1
        lastResolvedMark = SuperTrisRules.EMPTY
        microCelebrationToken = 0
        gameOverCelebrationToken = 0
    }
}
