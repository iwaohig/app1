package dev.moonlightfield.talktimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import dev.moonlightfield.talktimer.Links
import dev.moonlightfield.talktimer.R

/** App info and the privacy policy, which Google Play requires to be reachable in the app. */
@Composable
fun AboutScreen(
    versionName: String,
    onOpenPolicy: () -> Unit,
    onClose: () -> Unit,
) {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.app_name), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(stringResource(R.string.version, versionName), color = Color(0xFF8A8F98), fontSize = 11.sp)
            Text(stringResource(R.string.developer), color = Color(0xFFA6ABB3), fontSize = 11.sp, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.privacy_summary),
                color = Color(0xFFA6ABB3), fontSize = 11.sp, textAlign = TextAlign.Center,
            )
            Text(Links.PRIVACY_POLICY_URL, color = Color(0xFF8A8F98), fontSize = 9.sp, textAlign = TextAlign.Center)
            Chip(stringResource(R.string.privacy_open_on_phone), Color(0xFF2A2F37), onOpenPolicy)
            Chip(stringResource(R.string.close), Color(0xFF1F2329), onClose)
        }
    }
}

@Composable
private fun Chip(label: String, color: Color, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
