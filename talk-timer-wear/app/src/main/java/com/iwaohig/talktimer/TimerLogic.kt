package com.iwaohig.talktimer

import kotlin.math.ceil

/** Pure timer rules. No Android dependencies, so they are covered by unit tests. */
object TimerLogic {

    const val MIN_MINUTES = 1
    const val MAX_MINUTES = 180
    const val DEFAULT_MINUTES = 20

    /** Fixed remaining-time alerts (seconds) used after the halfway point. */
    private val STANDARD_ALERTS = listOf(600, 300, 180, 60)

    /** Minutes of overtime after which we stop buzzing every minute. */
    const val MAX_OVERTIME_ALERT_MINUTES = 60

    /**
     * Remaining-time alerts in seconds, largest first.
     * The halfway point, plus 10/5/3/1 minutes left when they come after the halfway point.
     *
     * 60 min -> 30, 10, 5, 3, 1 min
     * 30 min -> 15, 10, 5, 3, 1 min
     * 20 min -> 10, 5, 3, 1 min
     */
    fun alertPoints(durationSec: Int): List<Int> {
        val half = durationSec / 2
        return (listOf(half) + STANDARD_ALERTS)
            .filter { it in 1..half }
            .distinct()
            .sortedDescending()
    }

    enum class Pattern { SHORT_1, SHORT_2, SHORT_3, LONG }

    /** Last 3 min and last 1 min get distinct patterns so they can be told apart without looking. */
    fun patternFor(remainingSec: Int): Pattern = when (remainingSec) {
        60 -> Pattern.SHORT_3
        180 -> Pattern.SHORT_2
        else -> Pattern.SHORT_1
    }

    /** Vibration waveform: off/on durations in ms, starting with an initial delay. */
    fun waveform(pattern: Pattern): LongArray = when (pattern) {
        Pattern.SHORT_1 -> longArrayOf(0, 150)
        Pattern.SHORT_2 -> longArrayOf(0, 150, 150, 150)
        Pattern.SHORT_3 -> longArrayOf(0, 150, 150, 150, 150, 150)
        Pattern.LONG -> longArrayOf(0, 800)
    }

    enum class Stage { GO, WARN, STOP, OVER }

    /** Ring colour stage: green, yellow from 3 min left, red from 1 min left. */
    fun stage(remainingMs: Long): Stage = when {
        remainingMs <= 0 -> Stage.OVER
        remainingMs <= 60_000 -> Stage.STOP
        remainingMs <= 180_000 -> Stage.WARN
        else -> Stage.GO
    }

    /** "mm:ss" for the absolute value, rounding remaining time up so 0:00 only shows at the end. */
    fun formatClock(ms: Long): String {
        val totalSec = if (ms >= 0) ceil(ms / 1000.0).toLong() else (-ms) / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return "%02d:%02d".format(m, s)
    }

    /** Whole minutes for the ambient (low-power) screen, which only refreshes once a minute. */
    fun ambientMinutes(ms: Long): Long =
        if (ms >= 0) ceil(ms / 60_000.0).toLong() else (-ms) / 60_000

    fun clampMinutes(minutes: Int): Int = minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)
}
