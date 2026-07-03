package it.claudio.supertris

import android.app.Application
import it.claudio.supertris.data.GameRepository
import it.claudio.supertris.net.NearbyTransport
import it.claudio.supertris.net.OnlineRoomClient

class SuperTrisApplication : Application() {
    val gameRepository: GameRepository by lazy { GameRepository(this) }
    val nearbyTransport: NearbyTransport by lazy { NearbyTransport(this) }

    // False se la build non include google-services.json (vedi FIREBASE_SETUP.md).
    val onlineAvailable: Boolean by lazy { OnlineRoomClient.isAvailable(this) }
    val onlineRoomClient: OnlineRoomClient by lazy { OnlineRoomClient() }
}