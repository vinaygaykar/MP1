package io.github.vinaygaykar.mp1.ui.nowplaying.waveforms

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.sin

const val DEFAULT_WAVE_CYCLES = 3f

/** Default energy level when not provided. */
const val DEFAULT_ENERGY_LEVEL = 0.2f

/** Default aspect ratio for waveform calculations. */
const val DEFAULT_ASPECT_RATIO = 1.5f

/** Minimum allowed aspect ratio for wave width calculation. */
private const val MIN_ASPECT_RATIO = 1.0f

/** Maximum allowed aspect ratio for wave width calculation. */
private const val MAX_ASPECT_RATIO = 1.5f

/** Alpha transparency factor for the unplayed background track. */
private const val INACTIVE_TRACK_ALPHA = 0.2f

/** Extra padding added to wave canvas width bounds. */
private val STROKE_PADDING = 4.dp

/** Minimum threshold floor for wave cycle count. */
private const val MIN_CYCLE_COUNT_FLOOR = 1f

/** Constant representing 2 * PI in radians. */
private const val TWO_PI = 6.2831855f

/** Duration in milliseconds for play/pause amplitude animation transition. */
private const val PLAYBACK_ANIM_DURATION_MS = 500

/** Base maximum center-to-crest amplitude height. */
private val BASE_MAX_AMPLITUDE = 6.dp

/** Maximum radius of the circular scrubbing seek thumb head. */
private val SCRUB_THUMB_MAX_RADIUS = 8.dp

/** Fixed total height of the waveform canvas container. */
private val CANVAS_HEIGHT = 48.dp

/** Default stroke width (thickness) of the waveform path. */
private val DEFAULT_STROKE_WIDTH = 4.dp

/** Extra stroke width expansion added during scrubbing. */
private val SCRUB_STROKE_EXPANSION = 2.dp

/** Energy split threshold: below this energy ramps oscillation speed, above this energy adds wave cycles up to 3. */
private const val ENERGY_SPLIT_THRESHOLD = 0.6f

/** Fastest phase animation duration (500ms period = 2.0 Hz). */
private const val MIN_SPEED_MILLIS = 500

/** Slowest phase animation duration (4000ms period = 0.25 Hz). */
private const val MAX_SPEED_MILLIS = 4000

/** Minimum oscillation frequency (0.25 Hz = 4000ms period). */
private const val MIN_FREQ = 1.0f / MAX_SPEED_MILLIS

/** Maximum oscillation frequency (2.0 Hz = 500ms period). */
private const val MAX_FREQ = 1.0f / MIN_SPEED_MILLIS

/** Default height of the active waveform oscillation. */
private val WAVE_HEIGHT = 24.dp

/**
 * Dynamic energy-reactive waveform used for [io.github.vinaygaykar.mp1.domain.models.PlaybackWaveType.ENERGY].
 */
@Composable
fun EnergyWaveform(
    isPlaying: Boolean,
    progress: Float,
    scrubFactor: Float,
    modifier: Modifier = Modifier,
    energy: Float = DEFAULT_ENERGY_LEVEL,
    waveCycles: Float = DEFAULT_WAVE_CYCLES,
    amplitudeScale: Float? = null,
    aspectRatio: Float = DEFAULT_ASPECT_RATIO,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = INACTIVE_TRACK_ALPHA),
    thumbColor: Color = activeColor
) {
    val coercedEnergy = energy.coerceIn(0f, 1f)

    // For energy <= 0.6: Speed ramps 0.25Hz -> 2.0Hz (4000ms -> 500ms), cycles stay at 1.0
    // For energy > 0.6: Speed stays at 2.0Hz (500ms), cycles ramp from 1.0 to 3.0, clamped up (ceiled) to 0.5 steps
    val targetCycles = if (waveCycles != DEFAULT_WAVE_CYCLES) {
        waveCycles
    } else {
        val energyForCycles = ((coercedEnergy - ENERGY_SPLIT_THRESHOLD) / (1.0f - ENERGY_SPLIT_THRESHOLD)).coerceIn(0f, 1f)
        val rawCycles = 1.0f + (energyForCycles * 2.0f)
        ceil(rawCycles / 0.5f) * 0.5f
    }

    val animatedCycles by animateFloatAsState(
        targetValue = targetCycles,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "animatedCycles"
    )

    val targetAmpScale = amplitudeScale ?: 1.0f
    val animatedAmplitudeScale by animateFloatAsState(
        targetValue = targetAmpScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "animatedAmplitudeScale"
    )

    // Linear frequency ramp from 0.25 Hz (4000 ms) to 2.0 Hz (500 ms) over energy 0.0 -> 0.6
    val energyForSpeed = (coercedEnergy / ENERGY_SPLIT_THRESHOLD).coerceIn(0f, 1f)
    val currentFreq = MIN_FREQ + ((MAX_FREQ - MIN_FREQ) * energyForSpeed)
    val currentFreqState by rememberUpdatedState(currentFreq)

    val safeCycles = animatedCycles.coerceAtLeast(MIN_CYCLE_COUNT_FLOOR)
    val wavelength = WAVE_HEIGHT * aspectRatio.coerceIn(MIN_ASPECT_RATIO, MAX_ASPECT_RATIO)
    val targetWaveWidth = wavelength * safeCycles + STROKE_PADDING

    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos != 0L) {
                    val frameDeltaMillis = (frameNanos - lastFrameNanos) / 1_000_000f
                    val deltaPhase = TWO_PI * currentFreqState * frameDeltaMillis
                    phase = (phase + deltaPhase) % TWO_PI
                }
                lastFrameNanos = frameNanos
            }
        }
    }

    val amplitudeMultiplier by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(PLAYBACK_ANIM_DURATION_MS),
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

        // Base center-to-crest amplitude
        val baseMaxAmplitude = BASE_MAX_AMPLITUDE.toPx()
        val maxAmplitude = baseMaxAmplitude * animatedAmplitudeScale

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
            val angle = rel.toDouble() * animatedCycles * TWO_PI.toDouble() - phase.toDouble()
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
                val angle = rel.toDouble() * animatedCycles * TWO_PI.toDouble() - phase.toDouble()
                val sine = sin(angle).toFloat()
                val y = centerY + sine * currentAmplitude
                if (i == 0) activePath.moveTo(x, y) else activePath.lineTo(x, y)
            }

            val endAngle = coercedProgress.toDouble() * animatedCycles * TWO_PI.toDouble() - phase.toDouble()
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
            val thumbRadiusPx = SCRUB_THUMB_MAX_RADIUS.toPx() * scrubFactor
            val thumbX = startX + trackWidth * coercedProgress
            drawCircle(
                color = thumbColor,
                radius = thumbRadiusPx,
                center = Offset(thumbX, centerY)
            )
        }
    }
}
