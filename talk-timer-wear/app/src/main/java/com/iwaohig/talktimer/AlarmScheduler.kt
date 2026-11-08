package com.iwaohig.talktimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules every vibration as an alarm, so alerts fire on time even when the screen is off,
 * the app is in the background or the watch is in Doze.
 */
object AlarmScheduler {

    // Request codes. Alert points use slots 0..4 (there are at most 5).
    private const val SLOT_TIME_UP = 8
    private const val SLOT_OVERTIME = 9
    private const val MAX_ALERT_SLOTS = 5

    const val ACTION_ALERT = "com.iwaohig.talktimer.ALERT"
    const val EXTRA_KIND = "kind"
    const val EXTRA_REMAINING_SEC = "remainingSec"
    const val EXTRA_OVER_MINUTES = "overMinutes"
    const val EXTRA_END_AT = "endAt"

    const val KIND_POINT = 0
    const val KIND_TIME_UP = 1
    const val KIND_OVERTIME = 2

    fun schedule(context: Context, state: TimerState) {
        cancelAll(context)
        if (state.status != Status.RUNNING) return
        val now = System.currentTimeMillis()
        val endAt = state.endAtMillis

        TimerLogic.alertPoints(state.durationMinutes * 60).forEachIndexed { slot, sec ->
            val at = endAt - sec * 1000L
            if (at > now) {
                set(context, slot, at, alertIntent(context, KIND_POINT, endAt).putExtra(EXTRA_REMAINING_SEC, sec))
            }
        }

        if (endAt > now) {
            set(context, SLOT_TIME_UP, endAt, alertIntent(context, KIND_TIME_UP, endAt))
        } else {
            // Resumed while already over time: carry on with the per-minute reminders.
            val overMinutes = ((now - endAt) / 60_000L + 1).toInt()
            scheduleOvertime(context, endAt, overMinutes)
        }
    }

    fun scheduleOvertime(context: Context, endAt: Long, overMinutes: Int) {
        if (overMinutes > TimerLogic.MAX_OVERTIME_ALERT_MINUTES) return
        set(
            context,
            SLOT_OVERTIME,
            endAt + overMinutes * 60_000L,
            alertIntent(context, KIND_OVERTIME, endAt).putExtra(EXTRA_OVER_MINUTES, overMinutes),
        )
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val slots = (0 until MAX_ALERT_SLOTS) + SLOT_TIME_UP + SLOT_OVERTIME
        for (slot in slots) {
            val pi = PendingIntent.getBroadcast(
                context, slot, Intent(context, AlertReceiver::class.java).setAction(ACTION_ALERT),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            ) ?: continue
            am.cancel(pi)
            pi.cancel()
        }
    }

    private fun alertIntent(context: Context, kind: Int, endAt: Long) =
        Intent(context, AlertReceiver::class.java)
            .setAction(ACTION_ALERT)
            .putExtra(EXTRA_KIND, kind)
            .putExtra(EXTRA_END_AT, endAt)

    private fun set(context: Context, slot: Int, triggerAt: Long, intent: Intent) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            context, slot, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            // Alarm-clock alarms are exact and are not throttled in Doze.
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, OngoingNotifier.openAppIntent(context)), pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }
}
