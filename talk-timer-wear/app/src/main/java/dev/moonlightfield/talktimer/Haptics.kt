package dev.moonlightfield.talktimer

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object Haptics {

    /** Starts the vibration and returns how long it lasts in ms (0 if the watch cannot vibrate). */
    fun play(
        context: Context,
        pattern: TimerLogic.Pattern,
        strength: TimerLogic.Strength = Settings.get(context).vibration,
    ): Long {
        val vibrator = vibrator(context) ?: return 0
        if (!vibrator.hasVibrator()) return 0
        val timings = TimerLogic.waveform(pattern, strength)
        val effect = if (vibrator.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(timings, TimerLogic.amplitudes(pattern, strength), -1)
        } else {
            VibrationEffect.createWaveform(timings, -1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // "Alarm" usage keeps working while the watch is in do-not-disturb / theatre mode
            // as far as the system allows, which is what a speaker relies on.
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect)
        }
        return TimerLogic.durationMs(pattern, strength)
    }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
}
