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

const val DEFAULT_WAVE_CYCLES = 3f

@Composable
fun PlaybackWaveform(
    isPlaying: Boolean,
    progress: Float,
    scrubFactor: Float,
    modifier: Modifier = Modifier,
    energy: Float = 0.5f,
    energyDelta: Float = 0f,
    waveCycles: Float = DEFAULT_WAVE_CYCLES,
    amplitudeScale: Float? = null,
    aspectRatio: Float = 1.5f,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
    thumbColor: Color = activeColor
) {
    val canvasHeight = 36.dp
    val waveHeight = 24.dp // Peak-to-peak base amplitude height
    val strokePadding = 4.dp // Padding for end stroke caps

    val coercedEnergy = energy.coerceIn(0f, 1f)
    val coercedEnergyDelta = (energyDelta * 2.5f).coerceIn(0f, 1f)

    val targetCycles = if (waveCycles != DEFAULT_WAVE_CYCLES) {
        waveCycles
    } else {
        1.0f + (coercedEnergyDelta * 2.0f)
    }

    val animatedCycles by animateFloatAsState(
        targetValue = targetCycles,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "animatedCycles"
    )

    // Wave amplitude scale reduced by 50% (0.5x) of full base size
    val targetAmpScale = (amplitudeScale ?: 1.0f) * 0.5f
    val animatedAmplitudeScale by animateFloatAsState(
        targetValue = targetAmpScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "animatedAmplitudeScale"
    )

    // Oscillation speed driven dramatically by energy:
    // Low energy (0.0): ~12,000ms per phase cycle (almost stopped)
    // High energy (1.0): ~500ms per phase cycle (fast dramatic oscillation)
    val minFreq = 1.0f / 12000f
    val maxFreq = 1.0f / 500f
    val currentFreq = minFreq + ((maxFreq - minFreq) * coercedEnergy)
    val speedMillis = (1.0f / currentFreq).toInt().coerceIn(500, 12000)

    val safeCycles = animatedCycles.coerceAtLeast(0.1f)
    val wavelength = waveHeight * aspectRatio.coerceIn(1.0f, 1.5f)
    val targetWaveWidth = wavelength * safeCycles + strokePadding

    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(speedMillis, easing = LinearEasing),
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
            .height(canvasHeight)
            .widthIn(min = targetWaveWidth, max = targetWaveWidth)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val strokeWidthPx = 4.dp.toPx() + 2.dp.toPx() * scrubFactor // 4dp wave -> 6dp M3 flat progress bar
        val halfStroke = strokeWidthPx / 2f

        val baseMaxAmplitude = (height / 2f - halfStroke - 1.dp.toPx()).coerceAtLeast(2.dp.toPx())
        val maxAmplitude = baseMaxAmplitude * animatedAmplitudeScale

        val currentAmplitude = maxAmplitude * amplitudeMultiplier * (1f - scrubFactor)

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
            val angle = rel.toDouble() * animatedCycles * 2.0 * PI - phase.toDouble()
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
                val angle = rel.toDouble() * animatedCycles * 2.0 * PI - phase.toDouble()
                val sine = sin(angle).toFloat()
                val y = centerY + sine * currentAmplitude
                if (i == 0) activePath.moveTo(x, y) else activePath.lineTo(x, y)
            }

            // Ensure active path reaches the exact progress point
            val endAngle = coercedProgress.toDouble() * animatedCycles * 2.0 * PI - phase.toDouble()
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
