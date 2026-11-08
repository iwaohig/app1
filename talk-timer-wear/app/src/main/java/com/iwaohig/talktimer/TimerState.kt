package com.iwaohig.talktimer

enum class Status { IDLE, RUNNING, PAUSED }

/**
 * Everything needed to draw the timer. Time is stored as the wall-clock end time while running,
 * so the screen, alarms, tile and complication all agree even if the process is restarted.
 */
data class TimerState(
    val status: Status = Status.IDLE,
    val durationMinutes: Int = TimerLogic.DEFAULT_MINUTES,
    /** Wall-clock millis when the time is up. Valid while RUNNING. */
    val endAtMillis: Long = 0L,
    /** Remaining millis frozen at the moment of pausing (negative when paused in overtime). */
    val pausedRemainingMs: Long = 0L,
) {
    val durationMs: Long get() = durationMinutes * 60_000L

    fun remainingMs(now: Long): Long = when (status) {
        Status.IDLE -> durationMs
        Status.RUNNING -> endAtMillis - now
        Status.PAUSED -> pausedRemainingMs
    }
}
