package io.github.vinaygaykar.mp1.ui.library

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
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

/** Amplitude scale factor relative to base max height (0.4 = 40% height). */
private const val AMPLITUDE_SCALE = 0.4f

/** Aspect ratio multiplier determining wavelength relative to wave height. */
private const val ASPECT_RATIO = 1.5f

/** Extra padding in dp added to canvas width for end stroke caps. */
private val STROKE_PADDING = 4.dp

/** Fixed number of sine wave cycles rendered for simplicity mode. */
private const val WAVE_CYCLES = 1f

/** Duration in milliseconds for one full 2π phase oscillation cycle. */
private const val SPEED_MILLIS = 2000

/** Fixed total height of the waveform canvas container. */
private val CANVAS_HEIGHT = 88.dp

/** Default stroke width (thickness) of the waveform path. */
private val DEFAULT_STROKE_WIDTH = 4.dp

/** Extra stroke width expansion added during scrubbing. */
private val SCRUB_STROKE_EXPANSION = 2.dp

/** Peak-to-peak base height used for wavelength calculations. */
private val WAVE_HEIGHT = 24.dp

/**
 * Simplified 1-cycle sine wave progress bar used for [io.github.vinaygaykar.mp1.domain.models.PlaybackWave.SIMPLICITY].
 */
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
    val wavelength = WAVE_HEIGHT * ASPECT_RATIO
    val targetWaveWidth = wavelength * WAVE_CYCLES + STROKE_PADDING

    val infiniteTransition = rememberInfiniteTransition(label = "waveform_simplicity")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(SPEED_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val amplitudeMultiplier by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(500),
        label = "amplitude"
    )

    Canvas(
        modifier = modifier
            .height(CANVAS_HEIGHT)
            .widthIn(min = targetWaveWidth, max = targetWaveWidth)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val strokeWidthPx = DEFAULT_STROKE_WIDTH.toPx() + SCRUB_STROKE_EXPANSION.toPx() * scrubFactor
        val halfStroke = strokeWidthPx / 2f

        val baseMaxAmplitude = (height / 2f - halfStroke - 1.dp.toPx()).coerceAtLeast(2.dp.toPx())
        val maxAmplitude = baseMaxAmplitude * AMPLITUDE_SCALE
        val currentAmplitude = maxAmplitude * amplitudeMultiplier * (1f - scrubFactor)

        val startX = halfStroke
        val endX = (width - halfStroke).coerceAtLeast(startX)
        val trackWidth = endX - startX

        val coercedProgress = progress.coerceIn(0f, 1f)

        // Draw full background path
        val fullPath = Path()
        val steps = trackWidth.toInt().coerceAtLeast(1)
        for (i in 0..steps) {
            val rel = i.toFloat() / steps
            val x = startX + rel * trackWidth
            val angle = rel.toDouble() * WAVE_CYCLES * 2.0 * PI - phase.toDouble()
            val sine = sin(angle).toFloat()
            val y = centerY + sine * currentAmplitude
            if (i == 0) fullPath.moveTo(x, y) else fullPath.lineTo(x, y)
        }

        drawPath(
            path = fullPath,
            color = inactiveColor,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )

        // Draw active progress path
        if (coercedProgress > 0f) {
            val activePath = Path()
            val activeEndX = startX + trackWidth * coercedProgress
            val activeSteps = (steps * coercedProgress).toInt().coerceAtLeast(1)

            for (i in 0..activeSteps) {
                val rel = (i.toFloat() / steps).coerceAtMost(coercedProgress)
                val x = startX + rel * trackWidth
                val angle = rel.toDouble() * WAVE_CYCLES * 2.0 * PI - phase.toDouble()
                val sine = sin(angle).toFloat()
                val y = centerY + sine * currentAmplitude
                if (i == 0) activePath.moveTo(x, y) else activePath.lineTo(x, y)
            }

            val endAngle = coercedProgress.toDouble() * WAVE_CYCLES * 2.0 * PI - phase.toDouble()
            val endY = centerY + sin(endAngle).toFloat() * currentAmplitude
            activePath.lineTo(activeEndX, endY)

            drawPath(
                path = activePath,
                color = activeColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        // Circular Seek Head
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
