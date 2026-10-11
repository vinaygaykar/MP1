package io.github.vinaygaykar.mp1.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlayerTimestampBar(
    displayPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    scrubFactor: Float,
    rowWidthFraction: Float,
    recordY: Dp,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(rowWidthFraction)
            .offset(y = recordY - 80.dp)
            .alpha(progress),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatTime(displayPositionMs),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            color = MaterialTheme.colorScheme.primary
        )
        PlaybackWaveform(
            isPlaying = isPlaying,
            progress = if (durationMs > 0) displayPositionMs.toFloat() / durationMs else 0f,
            scrubFactor = scrubFactor,
            activeColor = MaterialTheme.colorScheme.primary,
            inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        )
        Text(
            text = formatTime(durationMs),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}
