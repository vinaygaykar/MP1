package io.github.vinaygaykar.mp1.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme

@Composable
fun PlayerTimestampBar(
    displayPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    scrubFactor: Float,
    rowWidthFraction: Float,
    recordY: Dp,
    progress: Float,
    modifier: Modifier = Modifier,
    energy: Float = 0.5f,
    waveCycles: Float = DEFAULT_WAVE_CYCLES,
    minBarWidth: Dp = 140.dp,
    frequencyFrame: io.github.vinaygaykar.mp1.utils.FrequencyFrame? = null
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth(rowWidthFraction)
            .offset(y = recordY - 80.dp)
            .alpha(progress),
        contentAlignment = Alignment.Center
    ) {
        val maxScreenWidth = this.maxWidth // 90% screen width ceiling
        val textPadding = 72.dp // Estimated width for left/right timestamps + horizontal padding
        val availWaveWidthMin = (minBarWidth - textPadding).coerceAtLeast(0.dp)

        val waveHeight = 16.dp
        val reqWidth1_5 = waveHeight * 1.5f * waveCycles + 4.dp
        val reqWidth1_0 = waveHeight * 1.0f * waveCycles + 4.dp

        val (baseBarWidth, selectedAspectRatio) = when {
            reqWidth1_5 <= availWaveWidthMin -> minBarWidth to 1.5f
            reqWidth1_0 <= availWaveWidthMin -> minBarWidth to 1.0f
            (reqWidth1_5 + textPadding) <= maxScreenWidth -> (reqWidth1_5 + textPadding) to 1.5f
            else -> maxScreenWidth to 1.0f
        }

        val finalBarWidth = lerp(baseBarWidth, maxScreenWidth, scrubFactor)

        Row(
            modifier = Modifier
                .width(finalBarWidth)
                .wrapContentWidth(Alignment.CenterHorizontally),
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
            if (frequencyFrame != null) {
                DualPeakFrequencyWaveform(
                    isPlaying = isPlaying,
                    progress = if (durationMs > 0) displayPositionMs.toFloat() / durationMs else 0f,
                    scrubFactor = scrubFactor,
                    frequencyFrame = frequencyFrame,
                    activeColor = MaterialTheme.colorScheme.primary,
                    inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                )
            } else {
                PlaybackWaveform(
                    isPlaying = isPlaying,
                    progress = if (durationMs > 0) displayPositionMs.toFloat() / durationMs else 0f,
                    scrubFactor = scrubFactor,
                    energy = 0f,
                    waveCycles = 1f,
                    amplitudeScale = 0.8f,
                    aspectRatio = selectedAspectRatio,
                    activeColor = MaterialTheme.colorScheme.primary,
                    inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                )
            }
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
}

@Preview(showBackground = true)
@Composable
fun PlayerTimestampBarPreview() {
    MusicPlayerTheme {
        PlayerTimestampBar(
            displayPositionMs = 75000L,
            durationMs = 210000L,
            isPlaying = true,
            scrubFactor = 0f,
            rowWidthFraction = 1f,
            recordY = 80.dp,
            progress = 1f,
            modifier = Modifier.padding(16.dp)
        )
    }
}
