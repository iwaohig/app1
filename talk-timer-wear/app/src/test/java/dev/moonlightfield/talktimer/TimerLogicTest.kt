package dev.moonlightfield.talktimer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerLogicTest {

    private fun minutes(vararg m: Int) = m.map { it * 60 }

    @Test
    fun alertPoints_matchAgreedExamples() {
        assertEquals(minutes(30, 10, 5, 3, 1), TimerLogic.alertPoints(60 * 60))
        assertEquals(minutes(15, 10, 5, 3, 1), TimerLogic.alertPoints(30 * 60))
        assertEquals(minutes(10, 5, 3, 1), TimerLogic.alertPoints(20 * 60))
    }

    @Test
    fun alertPoints_otherLengths() {
        assertEquals(listOf(22 * 60 + 30) + minutes(10, 5, 3, 1), TimerLogic.alertPoints(45 * 60))
        assertEquals(minutes(5, 3, 1), TimerLogic.alertPoints(10 * 60))
        assertEquals(listOf(150, 60), TimerLogic.alertPoints(5 * 60))
        assertEquals(listOf(60), TimerLogic.alertPoints(2 * 60))
        assertEquals(listOf(30), TimerLogic.alertPoints(1 * 60))
    }

    @Test
    fun patterns_distinguishLastMinutes() {
        assertEquals(TimerLogic.Pattern.SHORT_1, TimerLogic.patternFor(1800))
        assertEquals(TimerLogic.Pattern.SHORT_1, TimerLogic.patternFor(600))
        assertEquals(TimerLogic.Pattern.SHORT_1, TimerLogic.patternFor(300))
        assertEquals(TimerLogic.Pattern.SHORT_2, TimerLogic.patternFor(180))
        assertEquals(TimerLogic.Pattern.SHORT_3, TimerLogic.patternFor(60))
    }

    @Test
    fun stage_changesAtThreeAndOneMinute() {
        assertEquals(TimerLogic.Stage.GO, TimerLogic.stage(180_001))
        assertEquals(TimerLogic.Stage.WARN, TimerLogic.stage(180_000))
        assertEquals(TimerLogic.Stage.STOP, TimerLogic.stage(60_000))
        assertEquals(TimerLogic.Stage.OVER, TimerLogic.stage(0))
    }

    @Test
    fun formatClock_roundsRemainingUp() {
        assertEquals("60:00", TimerLogic.formatClock(3_600_000))
        assertEquals("00:01", TimerLogic.formatClock(1))
        assertEquals("00:00", TimerLogic.formatClock(0))
        assertEquals("01:05", TimerLogic.formatClock(-65_400))
        assertEquals("180:00", TimerLogic.formatClock(180 * 60_000L))
    }

    @Test
    fun ambientMinutes() {
        assertEquals(13L, TimerLogic.ambientMinutes(12 * 60_000L + 1))
        assertEquals(2L, TimerLogic.ambientMinutes(-2 * 60_000L - 5))
    }

    @Test
    fun remaining_byStatus() {
        val idle = TimerState(durationMinutes = 20)
        assertEquals(1_200_000L, idle.remainingMs(now = 123))
        val running = idle.copy(status = Status.RUNNING, endAtMillis = 10_000)
        assertEquals(4_000L, running.remainingMs(now = 6_000))
        val paused = idle.copy(status = Status.PAUSED, pausedRemainingMs = -5_000)
        assertEquals(-5_000L, paused.remainingMs(now = 99_999))
    }
}
