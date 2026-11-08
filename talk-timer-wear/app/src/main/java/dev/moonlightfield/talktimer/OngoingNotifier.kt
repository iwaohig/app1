package dev.moonlightfield.talktimer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status as OngoingStatus

/**
 * Shows the timer as a Wear OS Ongoing Activity: an icon on the watch face and in the app
 * launcher's "recents" that takes the speaker straight back to the timer.
 */
object OngoingNotifier {

    private const val CHANNEL_ID = "timer"
    private const val NOTIFICATION_ID = 1

    fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun show(context: Context, state: TimerState) {
        if (!canPost(context)) return
        ensureChannel(context)

        val now = System.currentTimeMillis()
        val remaining = state.remainingMs(now)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.app_name))
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent(context))

        // Ongoing-activity timers count on the elapsed-realtime clock.
        val elapsedNow = SystemClock.elapsedRealtime()
        val status = if (state.status == Status.RUNNING) {
            builder.setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(state.endAtMillis)
            OngoingStatus.Builder()
                .addTemplate("#time#")
                .addPart("time", OngoingStatus.TimerPart(elapsedNow + remaining))
                .build()
        } else {
            builder.setContentText(context.getString(R.string.paused))
            OngoingStatus.Builder()
                .addTemplate("#label# #time#")
                .addPart("label", OngoingStatus.TextPart(context.getString(R.string.paused)))
                .addPart("time", OngoingStatus.TextPart(TimerLogic.formatClock(remaining)))
                .build()
        }

        OngoingActivity.Builder(context, NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_timer)
            .setTouchIntent(openAppIntent(context))
            .setStatus(status)
            .build()
            .apply(context)

        @Suppress("MissingPermission") // checked in canPost()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
        )
    }
}
