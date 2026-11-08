package dev.moonlightfield.talktimer.tile

import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import dev.moonlightfield.talktimer.MainActivity
import dev.moonlightfield.talktimer.R
import dev.moonlightfield.talktimer.Status
import dev.moonlightfield.talktimer.TimerController
import dev.moonlightfield.talktimer.TimerLogic
import dev.moonlightfield.talktimer.TimerStore

/** Swipe-left card: shows the set length and starts the timer with one tap. */
class TimerTileService : TileService() {

    companion object {
        private const val RESOURCES_VERSION = "1"
        private const val ID_START = "start"
        private const val ID_FINISH = "finish"

        fun requestUpdate(context: Context) {
            // The updater binds to a service, which a BroadcastReceiver's own context is not
            // allowed to do (ReceiverCallNotAllowedException), so always go through the
            // application context. A failed refresh must never take the alert down with it.
            runCatching {
                getUpdater(context.applicationContext).requestUpdate(TimerTileService::class.java)
            }
        }
    }

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<TileBuilders.Tile> = CallbackToFutureAdapter.getFuture { completer ->
        // "Finish" is a load action: the tile asks for a fresh layout with this clickable id,
        // so the timer can be stopped right here without opening the app.
        if (requestParams.currentState.lastClickableId == ID_FINISH &&
            TimerStore.get(this).status != Status.IDLE
        ) {
            TimerController.reset(this)
        }
        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout()))
            .build()
        completer.set(tile)
        "TimerTile"
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> = CallbackToFutureAdapter.getFuture { completer ->
        completer.set(ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build())
        "TimerTileResources"
    }

    private fun layout(): LayoutElementBuilders.LayoutElement {
        val state = TimerStore.get(this)
        val running = state.status != Status.IDLE
        val big = if (running) {
            TimerLogic.formatClock(state.remainingMs(System.currentTimeMillis()))
        } else {
            TimerLogic.formatClock(state.durationMs)
        }
        val buttonLabel = getString(if (running) R.string.tile_open else R.string.tile_start)

        val launch = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(packageName)
                    .setClassName(MainActivity::class.java.name)
                    .apply {
                        if (!running) {
                            addKeyToExtraMapping(
                                MainActivity.EXTRA_ACTION,
                                ActionBuilders.AndroidStringExtra.Builder()
                                    .setValue(MainActivity.ACTION_START).build(),
                            )
                        }
                    }
                    .build()
            )
            .build()

        val buttons: LayoutElementBuilders.LayoutElement = if (running) {
            // While a talk is on: go back to the timer, or stop it outright after the talk.
            LayoutElementBuilders.Row.Builder()
                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                .addContent(pill(buttonLabel, 72f, 0xFF4CD17A.toInt(), 0xFF0B1A10.toInt(), ID_START, launch))
                .addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(8f)).build())
                .addContent(
                    pill(
                        getString(R.string.finish), 72f, 0xFF5A1F1B.toInt(), 0xFFFFFFFF.toInt(),
                        ID_FINISH, ActionBuilders.LoadAction.Builder().build(),
                    )
                )
                .build()
        } else {
            pill(buttonLabel, 100f, 0xFF4CD17A.toInt(), 0xFF0B1A10.toInt(), ID_START, launch)
        }

        val column = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(text(getString(R.string.app_name), 13f, 0xFF8A8F98.toInt()))
            .addContent(spacer(4f))
            .addContent(text(big, 34f, 0xFFFFFFFF.toInt(), bold = true))
            .addContent(spacer(10f))
            .addContent(buttons)
            .build()

        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .addContent(column)
            .build()
    }

    private fun pill(
        label: String,
        width: Float,
        background: Int,
        foreground: Int,
        id: String,
        action: ActionBuilders.Action,
    ) = LayoutElementBuilders.Box.Builder()
        .setWidth(dp(width))
        .setHeight(dp(44f))
        .setModifiers(
            ModifiersBuilders.Modifiers.Builder()
                .setClickable(ModifiersBuilders.Clickable.Builder().setId(id).setOnClick(action).build())
                .setBackground(
                    ModifiersBuilders.Background.Builder()
                        .setColor(argb(background))
                        .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(22f)).build())
                        .build()
                )
                .build()
        )
        .addContent(text(label, 16f, foreground, bold = true))
        .build()

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        LayoutElementBuilders.Text.Builder()
            .setText(value)
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setSize(sp(size))
                    .setColor(argb(color))
                    .setWeight(
                        if (bold) LayoutElementBuilders.FONT_WEIGHT_BOLD
                        else LayoutElementBuilders.FONT_WEIGHT_NORMAL
                    )
                    .build()
            )
            .build()

    private fun spacer(height: Float) =
        LayoutElementBuilders.Spacer.Builder().setHeight(dp(height)).build()
}
