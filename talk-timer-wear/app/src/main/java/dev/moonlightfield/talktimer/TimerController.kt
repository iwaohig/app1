package dev.moonlightfield.talktimer

import android.content.Context
import dev.moonlightfield.talktimer.complication.RemainingComplicationService
import dev.moonlightfield.talktimer.tile.TimerTileService

/** The only place that changes timer state. Keeps alarms, notification and widgets in step. */
object TimerController {

    fun toggle(context: Context) {
        when (TimerStore.get(context).status) {
            Status.RUNNING -> pause(context)
            Status.IDLE, Status.PAUSED -> start(context)
        }
    }

    fun start(context: Context) {
        val s = TimerStore.get(context)
        val now = System.currentTimeMillis()
        val endAt = when (s.status) {
            Status.IDLE -> now + s.durationMs
            Status.PAUSED -> now + s.pausedRemainingMs
            Status.RUNNING -> return
        }
        val running = s.copy(status = Status.RUNNING, endAtMillis = endAt, pausedRemainingMs = 0L)
        TimerStore.set(context, running)
        AlarmScheduler.schedule(context, running)
        OngoingNotifier.show(context, running)
        refreshWidgets(context)
    }

    fun pause(context: Context) {
        val s = TimerStore.get(context)
        if (s.status != Status.RUNNING) return
        val paused = s.copy(
            status = Status.PAUSED,
            pausedRemainingMs = s.endAtMillis - System.currentTimeMillis(),
        )
        TimerStore.set(context, paused)
        AlarmScheduler.cancelAll(context)
        OngoingNotifier.show(context, paused)
        refreshWidgets(context)
    }

    fun reset(context: Context) {
        val s = TimerStore.get(context)
        TimerStore.set(context, TimerState(durationMinutes = s.durationMinutes))
        AlarmScheduler.cancelAll(context)
        OngoingNotifier.cancel(context)
        refreshWidgets(context)
    }

    fun changeMinutes(context: Context, delta: Int) {
        val s = TimerStore.get(context)
        if (s.status != Status.IDLE) return
        val minutes = TimerLogic.clampMinutes(s.durationMinutes + delta)
        if (minutes == s.durationMinutes) return
        TimerStore.set(context, s.copy(durationMinutes = minutes))
        refreshWidgets(context)
    }

    fun refreshWidgets(context: Context) {
        TimerTileService.requestUpdate(context)
        RemainingComplicationService.requestUpdate(context)
    }
}
