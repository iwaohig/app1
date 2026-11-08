package com.iwaohig.talktimer.tile

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
import com.iwaohig.talktimer.MainActivity
import com.iwaohig.talktimer.R
import com.iwaohig.talktimer.Status
import com.iwaohig.talktimer.TimerLogic
import com.iwaohig.talktimer.TimerStore

/** Swipe-left card: shows the set length and starts the timer with one tap. */
class TimerTileService : TileService() {

    companion object {
        private const val RESOURCES_VERSION = "1"

        fun requestUpdate(context: Context) {
            getUpdater(context).requestUpdate(TimerTileService::class.java)
        }
    }

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<TileBuilders.Tile> = CallbackToFutureAdapter.getFuture { completer ->
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

        val button = LayoutElementBuilders.Box.Builder()
            .setWidth(dp(100f))
            .setHeight(dp(44f))
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder().setId("start").setOnClick(launch).build()
                    )
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(0xFF4CD17A.toInt()))
                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(22f)).build())
                            .build()
                    )
                    .build()
            )
            .addContent(text(buttonLabel, 16f, 0xFF0B1A10.toInt(), bold = true))
            .build()

        val column = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(text(getString(R.string.app_name), 13f, 0xFF8A8F98.toInt()))
            .addContent(spacer(4f))
            .addContent(text(big, 34f, 0xFFFFFFFF.toInt(), bold = true))
            .addContent(spacer(10f))
            .addContent(button)
            .build()

        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .addContent(column)
            .build()
    }

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
