package it.claudio.supertris

import android.app.Application
import it.claudio.supertris.data.GameRepository

class SuperTrisApplication : Application() {
    val gameRepository: GameRepository by lazy { GameRepository(this) }
}