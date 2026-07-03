package it.claudio.supertris

import android.app.Activity
import android.app.Application
import android.os.Bundle
import it.claudio.supertris.data.GameRepository
import it.claudio.supertris.net.NearbyTransport
import it.claudio.supertris.net.OnlineRoomClient
import it.claudio.supertris.notifications.TurnNotificationWorker

class SuperTrisApplication : Application() {
    val gameRepository: GameRepository by lazy { GameRepository(this) }
    val nearbyTransport: NearbyTransport by lazy { NearbyTransport(this) }

    // False se la build non include google-services.json (vedi FIREBASE_SETUP.md).
    val onlineAvailable: Boolean by lazy { OnlineRoomClient.isAvailable(this) }
    val onlineRoomClient: OnlineRoomClient by lazy { OnlineRoomClient() }

    // Il worker delle notifiche tace quando l'app e' visibile.
    private var visibleActivities = 0
    val isAppVisible: Boolean get() = visibleActivities > 0

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                visibleActivities++
            }

            override fun onActivityStopped(activity: Activity) {
                visibleActivities--
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })

        if (onlineAvailable) {
            TurnNotificationWorker.ensureChannel(this)
            TurnNotificationWorker.schedule(this)
        }
    }
}