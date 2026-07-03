package it.claudio.supertris.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import it.claudio.supertris.R
import it.claudio.supertris.SuperTrisApplication
import it.claudio.supertris.net.GameSummary
import it.claudio.supertris.net.GameSummaryStatus
import it.claudio.supertris.net.toSummary
import java.util.concurrent.TimeUnit

/**
 * Controllo periodico (~15 min, il minimo consentito da WorkManager) delle
 * partite online: se e' il tuo turno e non sei nell'app, arriva una notifica.
 * Alternativa a costo zero alle push FCM (che richiederebbero Cloud Functions
 * e piano a pagamento); il dedup per mossa evita notifiche ripetute.
 */
class TurnNotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SuperTrisApplication ?: return Result.success()
        if (!app.onlineAvailable) return Result.success()
        if (app.isAppVisible) return Result.success() // UI aperta: gia' aggiornata dai listener

        val nm = NotificationManagerCompat.from(applicationContext)
        if (!nm.areNotificationsEnabled()) return Result.success()

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val rooms = runCatching { app.onlineRoomClient.fetchMyGames(uid) }
            .getOrElse { return Result.retry() }

        val repo = app.gameRepository
        val notified = repo.readNotifiedMoves().toMutableMap()
        notified.keys.retainAll(rooms.map { it.code }.toSet())

        for (room in rooms) {
            val summary = room.toSummary(uid) ?: continue
            if (summary.status != GameSummaryStatus.YOUR_TURN) continue
            val moveCount = room.moves.size
            if (notified[room.code] == moveCount) continue

            notified[room.code] = moveCount
            runCatching { nm.notify(room.code.hashCode(), buildNotification(summary)) }
        }
        repo.saveNotifiedMoves(notified)
        return Result.success()
    }

    private fun buildNotification(summary: GameSummary): Notification {
        val ctx = applicationContext
        val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
            putExtra(EXTRA_OPEN_ONLINE, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            ctx,
            summary.code.hashCode(),
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = summary.opponentName
            ?.let { ctx.getString(R.string.notif_tuo_turno_testo, it) }
            ?: ctx.getString(R.string.notif_tuo_turno_testo_generico)

        return NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_grid)
            .setContentTitle(ctx.getString(R.string.notif_tuo_turno_titolo))
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "turni_online"
        const val EXTRA_OPEN_ONLINE = "open_online_lobby"
        private const val UNIQUE_WORK = "turn-check"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.notif_channel_desc)
                }
                context.getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(channel)
            }
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<TurnNotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
