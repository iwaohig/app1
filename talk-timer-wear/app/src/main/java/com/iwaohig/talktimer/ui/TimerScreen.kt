package com.iwaohig.talktimer.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.iwaohig.talktimer.R
import com.iwaohig.talktimer.Status
import com.iwaohig.talktimer.TimerLogic
import com.iwaohig.talktimer.TimerState
import kotlinx.coroutines.delay
import java.util.Date
import kotlin.math.cos
import kotlin.math.sin

private object Palette {
    val go = Color(0xFF4CD17A)
    val warn = Color(0xFFFFC23D)
    val stop = Color(0xFFFF5A4E)
    val stopDim = Color(0xFF5A1F1B)
    val track = Color(0xFF1F2329)
    val dim = Color(0xFF8A8F98)
    val soft = Color(0xFFA6ABB3)
    val ambient = Color(0xFF6B7078)
    val chip = Color(0xFF2A2F37)
}

private fun stageColor(stage: TimerLogic.Stage) = when (stage) {
    TimerLogic.Stage.GO -> Palette.go
    TimerLogic.Stage.WARN -> Palette.warn
    TimerLogic.Stage.STOP, TimerLogic.Stage.OVER -> Palette.stop
}

private val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun TimerScreen(
    state: TimerState,
    ambient: Boolean,
    ambientTick: Int,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onChangeMinutes: (Int) -> Unit,
) {
    // Redraw every second while interactive. Ambient mode redraws on ambientTick instead.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(ambient, ambientTick, state) {
        now = System.currentTimeMillis()
        if (ambient) return@LaunchedEffect
        while (true) {
            delay(1000 - System.currentTimeMillis() % 1000)
            now = System.currentTimeMillis()
        }
    }

    MaterialTheme {
        if (ambient) {
            AmbientContent(state, now)
        } else {
            InteractiveContent(state, now, onToggle, onReset, onChangeMinutes)
        }
    }
}

@Composable
private fun InteractiveContent(
    state: TimerState,
    now: Long,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onChangeMinutes: (Int) -> Unit,
) {
    val context = LocalContext.current
    val remaining = state.remainingMs(now)
    val over = remaining <= 0
    val blinkOn = (now / 500) % 2 == 0L
    val idle = state.status == Status.IDLE

    // Rotating the crown changes the length while idle. One detent is roughly 1 minute.
    val focusRequester = remember { FocusRequester() }
    var rotaryAccum by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onRotaryScrollEvent { event ->
                if (!idle) return@onRotaryScrollEvent false
                rotaryAccum += event.verticalScrollPixels
                val step = (rotaryAccum / 40f).toInt()
                if (step != 0) {
                    onChangeMinutes(step)
                    rotaryAccum -= step * 40f
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable()
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Ring(
            fraction = if (over) 1f else (remaining.toFloat() / state.durationMs).coerceIn(0f, 1f),
            color = if (over) (if (blinkOn) Palette.stop else Palette.stopDim)
            else stageColor(TimerLogic.stage(remaining)),
            strokeWidth = 7.dp,
            alertPoints = TimerLogic.alertPoints(state.durationMinutes * 60),
            durationSec = state.durationMinutes * 60,
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(if (over) R.string.label_over else R.string.label_left),
                color = if (over) Palette.stop else Palette.dim,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )

            if (idle) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundButton("−", stringResource(R.string.decrease)) { onChangeMinutes(-1) }
                    BigTime(TimerLogic.formatClock(remaining), Color.White, 40.sp)
                    RoundButton("+", stringResource(R.string.increase)) { onChangeMinutes(1) }
                }
            } else {
                BigTime(
                    text = (if (over) "+" else "") + TimerLogic.formatClock(remaining),
                    color = if (over) Palette.stop else Color.White,
                    size = 50.sp,
                )
            }

            val timeFormat = DateFormat.getTimeFormat(context)
            val nowText = timeFormat.format(Date(now))
            val timeLine = if (state.status == Status.RUNNING && !over) {
                stringResource(R.string.now_and_end, nowText, timeFormat.format(Date(state.endAtMillis)))
            } else {
                stringResource(R.string.now_only, nowText)
            }
            Text(
                text = timeLine,
                color = Palette.soft,
                fontSize = 11.sp,
                style = TabularNumbers,
                maxLines = 1,
            )

            Spacer(Modifier.height(6.dp))
            when (state.status) {
                Status.IDLE -> Text(
                    stringResource(R.string.tap_to_start),
                    color = Palette.go, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                )
                Status.PAUSED -> {
                    Text(
                        if (blinkOn) stringResource(R.string.paused) else "",
                        color = Palette.dim, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Palette.chip)
                            .clickable(onClick = onReset)
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                    ) {
                        Text(stringResource(R.string.reset), color = Color.White, fontSize = 11.sp)
                    }
                }
                Status.RUNNING -> Unit
            }
        }
    }
}

@Composable
private fun AmbientContent(state: TimerState, now: Long) {
    // Low-power screen: no colour, thin ring, whole minutes only (it refreshes once a minute).
    val context = LocalContext.current
    val remaining = state.remainingMs(now)
    val over = remaining <= 0
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Ring(
            fraction = if (over) 1f else (remaining.toFloat() / state.durationMs).coerceIn(0f, 1f),
            color = Palette.ambient,
            strokeWidth = 2.dp,
            alertPoints = emptyList(),
            durationSec = state.durationMinutes * 60,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(if (over) R.string.label_over else R.string.label_left),
                color = Palette.dim, fontSize = 12.sp,
            )
            val minutes = TimerLogic.ambientMinutes(remaining).toInt()
            BigTime(
                text = (if (over) "+" else "") + stringResource(R.string.minutes_short, minutes),
                color = Palette.soft,
                size = 40.sp,
                weight = FontWeight.Light,
            )
            Text(
                stringResource(R.string.now_only, DateFormat.getTimeFormat(context).format(Date(now))),
                color = Palette.ambient, fontSize = 11.sp, maxLines = 1,
            )
        }
    }
}

@Composable
private fun BigTime(
    text: String,
    color: Color,
    size: TextUnit,
    weight: FontWeight = FontWeight.Medium,
) {
    Text(
        text = text,
        color = color,
        fontSize = size,
        fontWeight = weight,
        style = TabularNumbers,
        maxLines = 1,
    )
}

@Composable
private fun RoundButton(label: String, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(26.dp)
            .clip(CircleShape)
            .background(Palette.chip)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 16.sp)
    }
}

/** Remaining-time ring around the screen edge, with ticks where the vibrations happen. */
@Composable
private fun Ring(
    fraction: Float,
    color: Color,
    strokeWidth: androidx.compose.ui.unit.Dp,
    alertPoints: List<Int>,
    durationSec: Int,
) {
    Canvas(Modifier.fillMaxSize().padding(3.dp)) {
        val stroke = strokeWidth.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        drawArc(Palette.track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        if (fraction > 0f) {
            drawArc(
                color, -90f, 360f * fraction, false, topLeft, arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        // Ticks: the ring shrinks counter-clockwise from 12 o'clock, so an alert with
        // `sec` seconds left sits at sec/duration of the way round.
        val r = size.width / 2
        for (sec in alertPoints) {
            val angle = Math.toRadians((sec.toDouble() / durationSec) * 360.0 - 90.0)
            val outer = r - 1.dp.toPx()
            val inner = r - stroke - 3.dp.toPx()
            drawLine(
                Color.White.copy(alpha = 0.6f),
                Offset(center.x + (inner * cos(angle)).toFloat(), center.y + (inner * sin(angle)).toFloat()),
                Offset(center.x + (outer * cos(angle)).toFloat(), center.y + (outer * sin(angle)).toFloat()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}
