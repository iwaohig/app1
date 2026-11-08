package dev.moonlightfield.talktimer.complication

import android.content.ComponentName
import android.content.Context
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.CountDownTimeReference
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText
import androidx.wear.watchface.complications.data.TimeDifferenceStyle
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import dev.moonlightfield.talktimer.OngoingNotifier
import dev.moonlightfield.talktimer.R
import dev.moonlightfield.talktimer.Status
import dev.moonlightfield.talktimer.TimerLogic
import dev.moonlightfield.talktimer.TimerStore
import java.time.Instant

/**
 * Shows the remaining time in any watch face slot. While running, the watch face counts down
 * by itself (time-difference text), so no per-second updates are needed from the app.
 */
class RemainingComplicationService : SuspendingComplicationDataSourceService() {

    companion object {
        fun requestUpdate(context: Context) {
            // Same reason as TimerTileService.requestUpdate: use the application context and
            // never let a refresh failure crash an alert.
            val app = context.applicationContext
            runCatching {
                ComplicationDataSourceUpdateRequester.create(
                    app, ComponentName(app, RemainingComplicationService::class.java),
                ).requestUpdateAll()
            }
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return shortText(plain("12:34"), getString(R.string.label_left))
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return null
        val state = TimerStore.get(this)
        val now = System.currentTimeMillis()
        return when (state.status) {
            Status.RUNNING -> {
                val text = TimeDifferenceComplicationText.Builder(
                    TimeDifferenceStyle.STOPWATCH,
                    CountDownTimeReference(Instant.ofEpochMilli(state.endAtMillis)),
                ).build()
                val over = state.endAtMillis <= now
                shortText(text, getString(if (over) R.string.label_over else R.string.label_left))
            }
            Status.PAUSED -> shortText(
                plain(TimerLogic.formatClock(state.pausedRemainingMs)),
                getString(R.string.paused),
            )
            Status.IDLE -> shortText(
                plain(TimerLogic.formatClock(state.durationMs)),
                getString(R.string.app_name_short),
            )
        }
    }

    private fun plain(text: String) = PlainComplicationText.Builder(text).build()

    private fun shortText(
        text: androidx.wear.watchface.complications.data.ComplicationText,
        title: String,
    ) = ShortTextComplicationData.Builder(text, plain(title))
        .setTitle(plain(title))
        .setTapAction(OngoingNotifier.openAppIntent(this))
        .build()
}
