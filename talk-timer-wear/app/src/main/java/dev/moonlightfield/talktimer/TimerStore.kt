package dev.moonlightfield.talktimer

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists [TimerState] so alarms and widgets still work after the app process is gone. */
object TimerStore {
    private const val PREFS = "timer"
    private const val KEY_STATUS = "status"
    private const val KEY_MINUTES = "minutes"
    private const val KEY_END_AT = "endAt"
    private const val KEY_PAUSED_REMAINING = "pausedRemaining"

    private var prefs: SharedPreferences? = null
    private val _state = MutableStateFlow(TimerState())

    fun state(context: Context): StateFlow<TimerState> {
        ensureLoaded(context)
        return _state.asStateFlow()
    }

    fun get(context: Context): TimerState {
        ensureLoaded(context)
        return _state.value
    }

    fun set(context: Context, state: TimerState) {
        ensureLoaded(context)
        _state.value = state
        prefs!!.edit {
            putString(KEY_STATUS, state.status.name)
            putInt(KEY_MINUTES, state.durationMinutes)
            putLong(KEY_END_AT, state.endAtMillis)
            putLong(KEY_PAUSED_REMAINING, state.pausedRemainingMs)
        }
    }

    @Synchronized
    private fun ensureLoaded(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _state.value = TimerState(
            status = runCatching { Status.valueOf(p.getString(KEY_STATUS, null) ?: "") }
                .getOrDefault(Status.IDLE),
            durationMinutes = TimerLogic.clampMinutes(
                p.getInt(KEY_MINUTES, TimerLogic.DEFAULT_MINUTES)
            ),
            endAtMillis = p.getLong(KEY_END_AT, 0L),
            pausedRemainingMs = p.getLong(KEY_PAUSED_REMAINING, 0L),
        )
    }
}
