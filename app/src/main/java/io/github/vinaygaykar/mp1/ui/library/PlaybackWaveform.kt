package io.github.vinaygaykar.mp1.ui.library

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun PlaybackWaveform(
    isPlaying: Boolean,
    progress: Float,
    scrubFactor: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
    thumbColor: Color = activeColor
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val amplitudeMultiplier by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(500),
        label = "amplitude"
    )

    Canvas(modifier = modifier.height(24.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val maxAmplitude = height / 3

        val currentAmplitude = maxAmplitude * amplitudeMultiplier * (1f - scrubFactor)
        val strokeWidthPx = 4.dp.toPx() + 2.dp.toPx() * scrubFactor // 4dp wave -> 6dp M3 flat progress bar
        val halfStroke = strokeWidthPx / 2f

        val startX = halfStroke
        val endX = (width - halfStroke).coerceAtLeast(startX)
        val trackWidth = endX - startX

        val coercedProgress = progress.coerceIn(0f, 1f)

        // Generate full background path
        val fullPath = Path()
        val steps = trackWidth.toInt().coerceAtLeast(1)
        for (i in 0..steps) {
            val rel = i.toFloat() / steps
            val x = startX + rel * trackWidth
            val angle = rel.toDouble() * 2.0 * PI - phase.toDouble()
            val sine = sin(angle).toFloat()
            val y = centerY + sine * currentAmplitude
            if (i == 0) fullPath.moveTo(x, y) else fullPath.lineTo(x, y)
        }

        // Draw background track
        drawPath(
            path = fullPath,
            color = inactiveColor,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )

        // Generate and draw active progress path
        if (coercedProgress > 0f) {
            val activePath = Path()
            val activeEndX = startX + trackWidth * coercedProgress
            val activeSteps = (steps * coercedProgress).toInt().coerceAtLeast(1)

            for (i in 0..activeSteps) {
                val rel = (i.toFloat() / steps).coerceAtMost(coercedProgress)
                val x = startX + rel * trackWidth
                val angle = rel.toDouble() * 2.0 * PI - phase.toDouble()
                val sine = sin(angle).toFloat()
                val y = centerY + sine * currentAmplitude
                if (i == 0) activePath.moveTo(x, y) else activePath.lineTo(x, y)
            }

            // Ensure active path reaches the exact progress point
            val endAngle = coercedProgress.toDouble() * 2.0 * PI - phase.toDouble()
            val endY = centerY + sin(endAngle).toFloat() * currentAmplitude
            activePath.lineTo(activeEndX, endY)

            drawPath(
                path = activePath,
                color = activeColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        // Circular Seek Head (appears only when scrubbing)
        if (scrubFactor > 0f) {
            val thumbRadiusPx = 8.dp.toPx() * scrubFactor
            val thumbX = startX + trackWidth * coercedProgress
            drawCircle(
                color = thumbColor,
                radius = thumbRadiusPx,
                center = Offset(thumbX, centerY)
            )
        }
    }
}
