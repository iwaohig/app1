package dev.moonlightfield.talktimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import dev.moonlightfield.talktimer.Prefs
import dev.moonlightfield.talktimer.R
import dev.moonlightfield.talktimer.TimerLogic

private val OnColor = Color(0xFF1E5A36)
private val OffColor = Color(0xFF2A2F37)
private val NoteColor = Color(0xFF8A8F98)

/** Settings, reached from the gear button on the idle timer screen. Each row toggles on tap. */
@Composable
fun SettingsScreen(
    prefs: Prefs,
    onToggleKeepScreenOn: () -> Unit,
    onCycleVibration: () -> Unit,
    onToggleLongPress: () -> Unit,
    onClose: () -> Unit,
) {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.settings), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))

            SettingRow(
                label = stringResource(R.string.keep_screen_on, onOff(prefs.keepScreenOn)),
                note = stringResource(R.string.keep_screen_on_note),
                highlighted = prefs.keepScreenOn,
                onClick = onToggleKeepScreenOn,
            )
            SettingRow(
                label = stringResource(R.string.vibration_strength, strengthLabel(prefs.vibration)),
                note = stringResource(R.string.vibration_strength_note),
                highlighted = true,
                onClick = onCycleVibration,
            )
            SettingRow(
                label = stringResource(R.string.long_press_to_pause, onOff(prefs.longPressToPause)),
                note = stringResource(R.string.long_press_to_pause_note),
                highlighted = prefs.longPressToPause,
                onClick = onToggleLongPress,
            )

            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.close),
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF1F2329))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SettingRow(label: String, note: String, highlighted: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(if (highlighted) OnColor else OffColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
    Text(note, color = NoteColor, fontSize = 10.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun onOff(on: Boolean) = stringResource(if (on) R.string.on else R.string.off)

@Composable
private fun strengthLabel(s: TimerLogic.Strength) = stringResource(
    when (s) {
        TimerLogic.Strength.WEAK -> R.string.strength_weak
        TimerLogic.Strength.NORMAL -> R.string.strength_normal
        TimerLogic.Strength.STRONG -> R.string.strength_strong
    },
)
