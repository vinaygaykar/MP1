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
import io.github.vinaygaykar.mp1.utils.FrequencyFrame

/** Fixed total height of the waveform canvas container. */
private val CANVAS_HEIGHT = 88.dp

/** Default stroke width (thickness) of the waveform path. */
private val DEFAULT_STROKE_WIDTH = 4.dp

/** Extra stroke width expansion added during scrubbing. */
private val SCRUB_STROKE_EXPANSION = 2.dp

/**
 * Frequency-reactive waveform used for [io.github.vinaygaykar.mp1.domain.models.PlaybackWave.FREQUENCY].
 */
@Composable
fun FrequencyWaveform(
    isPlaying: Boolean,
    progress: Float,
    scrubFactor: Float,
    frequencyFrame: FrequencyFrame,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
    thumbColor: Color = activeColor
) {
    val targetLow = if (isPlaying) (frequencyFrame.low * 1.2f).coerceIn(0f, 1f) else 0f
    val targetMid = if (isPlaying) (frequencyFrame.mid * 1.2f).coerceIn(0f, 1f) else 0f
    val targetHigh = if (isPlaying) (frequencyFrame.high * 1.2f).coerceIn(0f, 1f) else 0f

    val animSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val animatedLow by animateFloatAsState(targetValue = targetLow, animationSpec = animSpec, label = "animatedLow")
    val animatedMid by animateFloatAsState(targetValue = targetMid, animationSpec = animSpec, label = "animatedMid")
    val animatedHigh by animateFloatAsState(targetValue = targetHigh, animationSpec = animSpec, label = "animatedHigh")

    Canvas(
        modifier = modifier.height(CANVAS_HEIGHT)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        // Stroke width expands from 4dp to 6dp when scrubbing
        val strokeWidthPx = DEFAULT_STROKE_WIDTH.toPx() + SCRUB_STROKE_EXPANSION.toPx() * scrubFactor
        val halfStroke = strokeWidthPx / 2f

        val startX = halfStroke
        val endX = (width - halfStroke).coerceAtLeast(startX)
        val trackWidth = endX - startX

        // Amplitude drops to 0 when scrubbing (scrubFactor = 1f), morphing into a flat progressbar
        val morphFactor = 1f - scrubFactor
        val baseMaxAmp = (height / 2f - halfStroke - 1.dp.toPx()).coerceAtLeast(6.dp.toPx())
        val maxAmpPx = baseMaxAmp * morphFactor

        val lowAmp = maxAmpPx * animatedLow
        val midAmp = maxAmpPx * animatedMid
        val highAmp = maxAmpPx * animatedHigh

        // 3 Peaks + 2 Valleys X positions along track
        val x0 = startX
        val xPeak1 = startX + trackWidth * 0.166f
        val xValley1 = startX + trackWidth * 0.333f
        val xPeak2 = startX + trackWidth * 0.500f
        val xValley2 = startX + trackWidth * 0.666f
        val xPeak3 = startX + trackWidth * 0.833f
        val xEnd = endX

        // Dynamic Valley Depths: Valleys plunge deep on heavy beats and rise back up on quiet moments
        val valley1Depth = maxAmpPx * ((animatedLow + animatedMid) / 2f).coerceIn(0f, 1f) * 0.75f
        val valley2Depth = maxAmpPx * ((animatedMid + animatedHigh) / 2f).coerceIn(0f, 1f) * 0.75f

        // Key Y coordinates (Up is negative delta from centerY, valleys dip dynamically down)
        val y0 = centerY
        val yPeak1 = centerY - lowAmp                          // Left Peak (Low / Bass)
        val yValley1 = centerY + valley1Depth                  // Dynamic Valley 1
        val yPeak2 = centerY - midAmp                          // Middle Peak (Mid / Vocals)
        val yValley2 = centerY + valley2Depth                  // Dynamic Valley 2
        val yPeak3 = centerY - highAmp                         // Right Peak (High / Treble)
        val yEnd = centerY

        // Build continuous 3-peak path with 2 joining valleys
        val fullPath = Path().apply {
            moveTo(x0, y0)

            // Segment 1: Start -> Left Peak (Low)
            cubicTo(
                x0 + (xPeak1 - x0) * 0.5f, y0,
                xPeak1 - (xPeak1 - x0) * 0.3f, yPeak1,
                xPeak1, yPeak1
            )

            // Segment 2: Left Peak -> Valley 1
            cubicTo(
                xPeak1 + (xValley1 - xPeak1) * 0.4f, yPeak1,
                xValley1 - (xValley1 - xPeak1) * 0.4f, yValley1,
                xValley1, yValley1
            )

            // Segment 3: Valley 1 -> Middle Peak (Mid)
            cubicTo(
                xValley1 + (xPeak2 - xValley1) * 0.4f, yValley1,
                xPeak2 - (xPeak2 - xValley1) * 0.4f, yPeak2,
                xPeak2, yPeak2
            )

            // Segment 4: Middle Peak -> Valley 2
            cubicTo(
                xPeak2 + (xValley2 - xPeak2) * 0.4f, yPeak2,
                xValley2 - (xValley2 - xPeak2) * 0.4f, yValley2,
                xValley2, yValley2
            )

            // Segment 5: Valley 2 -> Right Peak (High)
            cubicTo(
                xValley2 + (xPeak3 - xValley2) * 0.4f, yValley2,
                xPeak3 - (xPeak3 - xValley2) * 0.4f, yPeak3,
                xPeak3, yPeak3
            )

            // Segment 6: Right Peak -> End
            cubicTo(
                xPeak3 + (xEnd - xPeak3) * 0.3f, yPeak3,
                xEnd - (xEnd - xPeak3) * 0.5f, yEnd,
                xEnd, yEnd
            )
        }

        // Draw full inactive background wave
        drawPath(
            path = fullPath,
            color = inactiveColor,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )

        // Draw active progress wave overlay
        val coercedProgress = progress.coerceIn(0f, 1f)
        if (coercedProgress > 0f) {
            val steps = (trackWidth * coercedProgress).toInt().coerceAtLeast(1)
            val totalSteps = trackWidth.toInt().coerceAtLeast(1)

            val activePath = Path()

            for (i in 0..steps) {
                val rel = (i.toFloat() / totalSteps).coerceAtMost(coercedProgress)
                val currX = startX + rel * trackWidth

                val currY = evaluate3PeakCurveY(
                    rel = rel,
                    y0 = y0,
                    yP1 = yPeak1,
                    yV1 = yValley1,
                    yP2 = yPeak2,
                    yV2 = yValley2,
                    yP3 = yPeak3,
                    yEnd = yEnd
                )

                if (i == 0) activePath.moveTo(currX, currY) else activePath.lineTo(currX, currY)
            }

            drawPath(
                path = activePath,
                color = activeColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        // Draw circular seek head thumb when scrubbing
        if (scrubFactor > 0f) {
            val thumbRadiusPx = 8.dp.toPx() * scrubFactor
            val thumbX = startX + trackWidth * coercedProgress
            val thumbY = centerY
            drawCircle(
                color = thumbColor,
                radius = thumbRadiusPx,
                center = Offset(thumbX, thumbY)
            )
        }
    }
}

private fun evaluate3PeakCurveY(
    rel: Float,
    y0: Float,
    yP1: Float,
    yV1: Float,
    yP2: Float,
    yV2: Float,
    yP3: Float,
    yEnd: Float
): Float {
    return when {
        rel <= 0.166f -> {
            val s = rel / 0.166f
            cubicBezier1D(y0, y0, yP1, yP1, s)
        }
        rel <= 0.333f -> {
            val s = (rel - 0.166f) / 0.167f
            cubicBezier1D(yP1, yP1, yV1, yV1, s)
        }
        rel <= 0.500f -> {
            val s = (rel - 0.333f) / 0.167f
            cubicBezier1D(yV1, yV1, yP2, yP2, s)
        }
        rel <= 0.666f -> {
            val s = (rel - 0.500f) / 0.166f
            cubicBezier1D(yP2, yP2, yV2, yV2, s)
        }
        rel <= 0.833f -> {
            val s = (rel - 0.666f) / 0.167f
            cubicBezier1D(yV2, yV2, yP3, yP3, s)
        }
        else -> {
            val s = (rel - 0.833f) / 0.167f
            cubicBezier1D(yP3, yP3, yEnd, yEnd, s)
        }
    }
}

private fun cubicBezier1D(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
    val u = 1f - t
    val tt = t * t
    val uu = u * u
    val uuu = uu * u
    val ttt = tt * t

    return uuu * p0 + 3f * uu * t * p1 + 3f * u * tt * p2 + ttt * p3
}
