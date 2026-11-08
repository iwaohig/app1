package dev.moonlightfield.talktimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires the vibration for one alert, and chains the next overtime reminder. */
class AlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_ALERT) return
        val state = TimerStore.get(context)
        val endAt = intent.getLongExtra(AlarmScheduler.EXTRA_END_AT, -1L)
        // Ignore alarms left over from a run that was paused, reset or restarted.
        if (state.status != Status.RUNNING || state.endAtMillis != endAt) return

        when (intent.getIntExtra(AlarmScheduler.EXTRA_KIND, -1)) {
            AlarmScheduler.KIND_POINT -> {
                val sec = intent.getIntExtra(AlarmScheduler.EXTRA_REMAINING_SEC, 0)
                Haptics.play(context, TimerLogic.patternFor(sec))
                // The ring colour shown by the tile/complication changes at 3 min and 1 min left.
                if (sec == 180 || sec == 60) TimerController.refreshWidgets(context)
            }
            AlarmScheduler.KIND_TIME_UP -> {
                Haptics.play(context, TimerLogic.Pattern.LONG)
                AlarmScheduler.scheduleOvertime(context, endAt, 1)
                TimerController.refreshWidgets(context)
            }
            AlarmScheduler.KIND_OVERTIME -> {
                val over = intent.getIntExtra(AlarmScheduler.EXTRA_OVER_MINUTES, 1)
                Haptics.play(context, TimerLogic.Pattern.LONG)
                AlarmScheduler.scheduleOvertime(context, endAt, over + 1)
            }
        }
    }
}
