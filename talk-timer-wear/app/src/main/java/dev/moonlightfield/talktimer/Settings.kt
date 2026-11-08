package dev.moonlightfield.talktimer

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** User preferences that are not part of the timer itself. */
data class Prefs(
    /** Keep the display on (no ambient mode) while the timer is running. */
    val keepScreenOn: Boolean = true,
    val vibration: TimerLogic.Strength = TimerLogic.Strength.NORMAL,
    /** While running, a tap does nothing and pausing needs a long press. */
    val longPressToPause: Boolean = true,
)

object Settings {
    private const val PREFS = "settings"
    private const val KEY_KEEP_SCREEN_ON = "keepScreenOn"
    private const val KEY_VIBRATION = "vibration"
    private const val KEY_LONG_PRESS_TO_PAUSE = "longPressToPause"

    private var prefs: SharedPreferences? = null
    private val _state = MutableStateFlow(Prefs())

    fun state(context: Context): StateFlow<Prefs> {
        ensureLoaded(context)
        return _state.asStateFlow()
    }

    fun get(context: Context): Prefs {
        ensureLoaded(context)
        return _state.value
    }

    fun update(context: Context, change: (Prefs) -> Prefs) {
        ensureLoaded(context)
        val next = change(_state.value)
        _state.value = next
        prefs!!.edit {
            putBoolean(KEY_KEEP_SCREEN_ON, next.keepScreenOn)
            putString(KEY_VIBRATION, next.vibration.name)
            putBoolean(KEY_LONG_PRESS_TO_PAUSE, next.longPressToPause)
        }
    }

    @Synchronized
    private fun ensureLoaded(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        val defaults = Prefs()
        _state.value = Prefs(
            keepScreenOn = p.getBoolean(KEY_KEEP_SCREEN_ON, defaults.keepScreenOn),
            vibration = runCatching { TimerLogic.Strength.valueOf(p.getString(KEY_VIBRATION, null) ?: "") }
                .getOrDefault(defaults.vibration),
            longPressToPause = p.getBoolean(KEY_LONG_PRESS_TO_PAUSE, defaults.longPressToPause),
        )
    }
}
