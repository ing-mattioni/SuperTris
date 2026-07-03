package it.claudio.supertris

import android.app.Application
import it.claudio.supertris.data.GameRepository
import it.claudio.supertris.net.NearbyTransport

class SuperTrisApplication : Application() {
    val gameRepository: GameRepository by lazy { GameRepository(this) }
    val nearbyTransport: NearbyTransport by lazy { NearbyTransport(this) }
}