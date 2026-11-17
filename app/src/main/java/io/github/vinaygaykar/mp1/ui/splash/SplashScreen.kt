package io.github.vinaygaykar.mp1.ui.splash

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.vinaygaykar.mp1.R
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Total duration of the splash screen animation sequence in milliseconds.
 * Modify this constant to adjust the overall speed/timing of the animation.
 * All phase times below are authored on a [SPLASH_TIMELINE_SECONDS] timeline and are scaled to fit this.
 */
const val SPLASH_ANIMATION_DURATION_MS: Long = 3000L

/** Length of the authored timeline in seconds. Phase times below are relative to this. */
private const val SPLASH_TIMELINE_SECONDS = 3.0f

/**
 * Size of the center logo icon, matching the native splash screen.
 * Android 12+ draws the splash icon in a 288dp box when no icon background color is set
 * (240dp when one is set). Adjust this if it differs from what you see on the native splash.
 */
val SPLASH_LOGO_SIZE: Dp = 288.dp

/** The vector's artwork fills 72% of its box, so this is the visible size of the logo disc. */
private const val SPLASH_LOGO_VISIBLE_RATIO = 0.72f

/** Diameter of every album art: exactly the visible size of the logo. */
val SPLASH_ALBUM_ART_SIZE: Dp = SPLASH_LOGO_SIZE * SPLASH_LOGO_VISIBLE_RATIO

/**
 * Minimum gap between the logo and the close ring of album arts.
 * No album art can pop up within this buffer boundary.
 */
val SPLASH_LOGO_EXCLUSION_PADDING: Dp = 40.dp

// --- Logo (A) ---
private const val SPLASH_LOGO_GROWTH = 0.18f              // extra scale after all Bs are absorbed
private const val SPLASH_SPIN_START = 0.45f
private const val SPLASH_SPIN_TURNS = 4                   // whole turns in total, so the logo always rests upright (0 deg)
private const val SPLASH_SPIN_CURVE = 1.6f
private const val SPLASH_SPIN_STOP_DURATION = 0.20f   // ease-out length, ends exactly when the logo starts moving up

// --- Bs: count and size ---
private const val SPLASH_B_COUNT = 50                     // album arts are repeated to reach this count
private const val SPLASH_INITIAL_VISIBLE = 6              // popup Bs; the rest come from the screen edges

// --- Phase 1: spawn / placement ---
private const val SPLASH_SPAWN_STAGGER = 0.04f
private const val SPLASH_SPAWN_DURATION = 0.25f
private const val SPLASH_INNER_LAYER_COUNT = 6            // popup Bs on the close ring (rest go to the loose layer)
private const val SPLASH_POPUP_ANGLE_SPREAD = 0.1f        // random angle variation on the close ring (fraction of one slice)
private val SPLASH_INNER_JITTER = 16.dp                   // random extra distance on the close ring
private val SPLASH_OUTER_LAYER_MIN = 175.dp               // loose-layer Bs sit at least this far from the logo center
private val SPLASH_POPUP_GAP = 8.dp                       // min spacing between popup Bs
private val SPLASH_SCREEN_PADDING = 8.dp                  // popup Bs stay this far from the screen edge
private const val SPLASH_PLACE_ATTEMPTS = 40
private val SPLASH_OUTSIDE_MARGIN = 16.dp                 // how far past the edge hidden Bs wait
private const val SPLASH_EDGE_PEEK_COUNT = 6              // edge Bs that peek in early
private const val SPLASH_EDGE_PEEK_INSET = 0.3f           // fraction of radius a peeking B sits inside the edge
private const val SPLASH_EDGE_PEEK_START = 0.50f
private const val SPLASH_EDGE_PEEK_STAGGER = 0.08f

// --- Phase 2: pull ---
private val SPLASH_PULL_GROUPS = listOf(1, 1, 2, 3)       // early one-by-one pulls (popup Bs)
private val SPLASH_PULL_GROUP_TIMES = listOf(0.40f, 0.62f, 0.80f, 0.95f, 1.05f) // last entry = start of final cascade
private const val SPLASH_CASCADE_STAGGER = 0.014f
private const val SPLASH_PULL_DURATION = 0.40f            // popup Bs
private const val SPLASH_OUTSIDE_PULL_DURATION = 0.55f    // Bs coming from the screen edges
private const val SPLASH_PULL_ACCEL_POWER = 2.2f          // higher = stronger acceleration
private const val SPLASH_B_END_SHRINK = 0.75f
private const val SPLASH_B_SQUISH = 0.30f                 // stretch along motion

// --- Phase 3: bounce + exit ---
private const val SPLASH_BOUNCE_DAMPING = 11f             // higher = less bouncy
private const val SPLASH_BOUNCE_FREQUENCY = 17f
private const val SPLASH_EXIT_START = 2.40f
private const val SPLASH_LOGO_EXIT_SQUISH = 0.25f

private val SPLASH_PREVIEW_COLORS = listOf(
    Color(0xFF673AB7), Color(0xFFE91E63), Color(0xFF00BCD4),
    Color(0xFFFF9800), Color(0xFF4CAF50), Color(0xFF3F51B5),
    Color(0xFF9C27B0), Color(0xFFFF5722), Color(0xFFFF5722),
    Color(0xFFFFC107)
)

private class BallSpec(
    val uriIndex: Int, val radius: Float, val x: Float, val y: Float,   // x, y: start offset from logo center (px)
    val spawnT: Float, val pullT: Float, val pullDur: Float
)

private class BallFrame(
    val x: Float, val y: Float, val scale: Float, val squish: Float,
    val angleDeg: Float, val visible: Boolean
)

private class LogoSpinFrame(val spinDeg: Float, val growth: Float)

private class LogoExitFrame(val offsetY: Float, val scaleX: Float, val scaleY: Float)

private fun clamp01(v: Float) = v.coerceIn(0f, 1f)

private fun easeOutBack(x: Float): Float {
    val c = 1.9f
    return 1f + (c + 1f) * (x - 1f).pow(3) + c * (x - 1f).pow(2)
}

/**
 * Phase 1: Album arts pop up around the logo (logo is static).
 * Returns the pop-in scale of a single album art.
 */
private fun calculatePhase1(t: Float, b: BallSpec): Float {
    if (b.spawnT < 0f) return 1f // hidden-edge Bs are always "present"
    return easeOutBack(clamp01((t - b.spawnT) / SPLASH_SPAWN_DURATION))
}

/**
 * Phase 2 (album art): Album arts rotate towards the logo and are pulled in, squished along the motion.
 */
private fun GraphicsLayerScope.calculatePhase2(t: Float, b: BallSpec, popScale: Float): BallFrame {
    val p = clamp01((t - b.pullT) / b.pullDur)
    val e = p.pow(SPLASH_PULL_ACCEL_POWER) // position progress (accelerating)
    val v = if (p > 0f && p < 1f) p.pow(SPLASH_PULL_ACCEL_POWER - 1f) else 0f // speed proxy
    return BallFrame(
        x = b.x * (1f - e),
        y = b.y * (1f - e),
        scale = popScale * (1f - SPLASH_B_END_SHRINK * p.pow(3)),
        squish = SPLASH_B_SQUISH * v,
        angleDeg = Math.toDegrees(atan2(-b.y, -b.x).toDouble()).toFloat(),
        visible = p < 1f
    )
}

/**
 * Phase 2 (logo): Logo spins with increasing speed and grows slightly as album arts are absorbed.
 * The spin eases out once the last album art lands and stops upright right before the logo moves up.
 */
private fun calculatePhase2(t: Float, balls: List<BallSpec>, lastArrival: Float): LogoSpinFrame {
    var absorbed = 0f
    for (b in balls) absorbed += clamp01((t - b.pullT) / b.pullDur).pow(SPLASH_PULL_ACCEL_POWER)
    val growth = if (balls.isEmpty()) 1f else 1f + SPLASH_LOGO_GROWTH * absorbed / balls.size
    return LogoSpinFrame(spinDeg = spinAngle(t, lastArrival), growth = growth)
}

/**
 * Accelerating spin until the last album art lands, then constant deceleration to a stop exactly at
 * [SPLASH_EXIT_START]. The strength is derived from [SPLASH_SPIN_TURNS] so the final angle is a whole number of turns.
 */
private fun spinAngle(t: Float, lastArrival: Float): Float {
    val stopStart = min(lastArrival, SPLASH_EXIT_START - SPLASH_SPIN_STOP_DURATION)
    val stopDur = SPLASH_EXIT_START - stopStart
    val x = stopStart - SPLASH_SPIN_START
    val c = SPLASH_SPIN_CURVE
    // angle at rest = k * (x^c + c * x^(c-1) * stopDur / 2) = whole turns
    val k = 360f * SPLASH_SPIN_TURNS / (x.pow(c) + c * x.pow(c - 1f) * stopDur / 2f)
    return when {
        t <= SPLASH_SPIN_START -> 0f
        t <= stopStart -> k * (t - SPLASH_SPIN_START).pow(c)
        else -> {
            val tau = min(t - stopStart, stopDur)
            val v0 = k * c * x.pow(c - 1f)
            k * x.pow(c) + v0 * tau - 0.5f * (v0 / stopDur) * tau * tau
        }
    }
}

/**
 * Phase 3: Logo bounces back to its original size (damped), then flies up and off the screen.
 */
private fun GraphicsLayerScope.calculatePhase3(t: Float, growth: Float, lastArrival: Float, hPx: Float): LogoExitFrame {
    val bounce = if (t > lastArrival) {
        val d = t - lastArrival
        1f + (growth - 1f) * exp(-SPLASH_BOUNCE_DAMPING * d) * cos(SPLASH_BOUNCE_FREQUENCY * d) // no Bs, no bounce
    } else growth

    val q = clamp01((t - SPLASH_EXIT_START) / (SPLASH_TIMELINE_SECONDS - SPLASH_EXIT_START))
    val v = if (q > 0f) q * q else 0f // speed proxy
    val squish = SPLASH_LOGO_EXIT_SQUISH * v
    val travel = hPx / 2f +
        (SPLASH_LOGO_SIZE.toPx() / 2f) * (1f + SPLASH_LOGO_GROWTH) * (1f + SPLASH_LOGO_EXIT_SQUISH)
    return LogoExitFrame(
        offsetY = -travel * q.pow(3),
        scaleX = bounce * (1f - squish * 0.6f),
        scaleY = bounce * (1f + squish)
    )
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SplashComposeScreen(
    albumArtUris: List<Uri>,
    startAnimation: Boolean = true,
    modifier: Modifier = Modifier,
    animationDurationMs: Long = SPLASH_ANIMATION_DURATION_MS,
    onAnimationEnd: () -> Unit = {}
) {
    val isPreview = LocalInspectionMode.current
    val animProgress = remember { Animatable(if (isPreview) 0.35f else 0f) }
    val currentOnAnimationEnd = rememberUpdatedState(onAnimationEnd)

    LaunchedEffect(startAnimation) {
        if (!isPreview && startAnimation) {
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = animationDurationMs.toInt(),
                    easing = LinearEasing
                )
            )
            currentOnAnimationEnd.value() // logo has left the screen
        }
    }

    // Read only inside graphicsLayer blocks so each frame does not recompose the whole splash
    val timeSeconds = { animProgress.value * SPLASH_TIMELINE_SECONDS }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val balls = remember(albumArtUris.size, wPx, hPx) {
            density.buildBalls(albumArtUris.size, wPx, hPx)
        }
        val lastArrival = remember(balls) {
            balls.maxOfOrNull { it.pullT + it.pullDur } ?: SPLASH_EXIT_START
        }

        // 1. Album arts (drawn below the logo so they disappear into it)
        balls.forEach { ball ->
            SplashAlbumArt(
                ball = ball,
                uri = albumArtUris.getOrNull(ball.uriIndex),
                time = timeSeconds,
                isPreview = isPreview
            )
        }

        // 2. Center Icon (ic_icon_og) matching native splash screen dimensions (150dp rendered)
        SplashLogo(balls = balls, lastArrival = lastArrival, hPx = hPx, time = timeSeconds)
    }
}

@Composable
private fun SplashAlbumArt(ball: BallSpec, uri: Uri?, time: () -> Float, isPreview: Boolean) {
    val size = with(LocalDensity.current) { (ball.radius * 2).toDp() }
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                val t = time()
                val f = calculatePhase2(t, ball, calculatePhase1(t, ball))
                translationX = f.x
                translationY = f.y
                scaleX = f.scale * (1f + f.squish)
                scaleY = f.scale * (1f - f.squish * 0.6f)
                rotationZ = f.angleDeg // scale is applied before rotation, so the stretch follows the motion
                alpha = if (f.visible) 1f else 0f
            }
            .clip(CircleShape)
            .background(
                if (isPreview) SPLASH_PREVIEW_COLORS[ball.uriIndex % SPLASH_PREVIEW_COLORS.size]
                else Color.DarkGray
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isPreview) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(size * 0.45f)
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SplashLogo(balls: List<BallSpec>, lastArrival: Float, hPx: Float, time: () -> Float) {
    Box(
        modifier = Modifier
            .size(SPLASH_LOGO_SIZE)
            .graphicsLayer { // position / scale (no rotation here, so the exit stretch stays vertical)
                val t = time()
                val f = calculatePhase3(t, calculatePhase2(t, balls, lastArrival).growth, lastArrival, hPx)
                translationY = f.offsetY
                scaleX = f.scaleX
                scaleY = f.scaleY
            }
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_icon_og),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = calculatePhase2(time(), balls, lastArrival).spinDeg } // spin
        )
    }
}

private class Slot(val x: Float, val y: Float, val r: Float) {
    var spawnT = -1f
    var pullT = 0f
    var dur = SPLASH_PULL_DURATION
}

/**
 * Lays out and schedules the album arts around the logo.
 * Popup Bs (one per album art, up to [SPLASH_INITIAL_VISIBLE]) sit on two layers around the logo; the remaining Bs
 * (album arts repeated up to [SPLASH_B_COUNT]) wait beyond the screen edge, a few peeking in early.
 */
private fun Density.buildBalls(uriCount: Int, w: Float, h: Float): List<BallSpec> {
    if (uriCount == 0) return emptyList()

    val random = Random.Default // new layout on every launch
    val tau = (2 * PI).toFloat()
    val rLogo = SPLASH_LOGO_SIZE.toPx() * SPLASH_LOGO_VISIBLE_RATIO / 2f // visible logo disc
    val gap = SPLASH_POPUP_GAP.toPx()
    fun rand(a: Float, b: Float) = a + random.nextFloat() * (b - a)
    fun radius(): Float = SPLASH_ALBUM_ART_SIZE.toPx() / 2f // every album art has the same size
    // distance from center to the screen edge along ang (negative pad = beyond the edge)
    fun edgeReach(ang: Float, pad: Float): Float {
        val c = abs(cos(ang)).coerceAtLeast(1e-6f)
        val s = abs(sin(ang)).coerceAtLeast(1e-6f)
        return min((w / 2f - pad) / c, (h / 2f - pad) / s)
    }

    // popup Bs never overlap the logo or each other
    val placed = mutableListOf<Slot>()
    fun place(r: Float, gen: (Float) -> Pair<Float, Float>): Slot {
        var p = gen(r)
        for (n in 0 until SPLASH_PLACE_ATTEMPTS) {
            p = gen(r)
            val clear = hypot(p.first, p.second) > rLogo + r + gap &&
                placed.all { hypot(it.x - p.first, it.y - p.second) > it.r + r + gap }
            if (clear) break
        }
        return Slot(p.first, p.second, r).also { placed += it }
    }

    val initialVisible = min(SPLASH_INITIAL_VISIBLE, uriCount)
    val innerN = min(SPLASH_INNER_LAYER_COUNT, initialVisible)
    val outerN = initialVisible - innerN
    val off1 = rand(0f, tau)
    val off2 = rand(0f, tau)
    val off3 = rand(0f, tau)
    val popups = mutableListOf<Slot>()

    // layer 1: close ring around the logo
    for (i in 0 until innerN) {
        val a0 = off1 + (i + 0.5f) * tau / innerN
        val sw = SPLASH_POPUP_ANGLE_SPREAD * tau / innerN
        popups += place(radius()) { r ->
            val ang = a0 + rand(-0.5f, 0.5f) * sw
            val d = rLogo + r + SPLASH_LOGO_EXCLUSION_PADDING.toPx() + rand(0f, SPLASH_INNER_JITTER.toPx())
            cos(ang) * d to sin(ang) * d
        }
    }
    // layer 2: looser layer reaching toward the screen edges
    for (i in 0 until outerN) {
        val a0 = off2 + (i + 0.5f) * tau / outerN
        val sw = 0.8f * tau / outerN
        popups += place(radius()) { r ->
            val ang = a0 + rand(-0.5f, 0.5f) * sw
            val maxD = edgeReach(ang, r + SPLASH_SCREEN_PADDING.toPx())
            val d = rand(min(SPLASH_OUTER_LAYER_MIN.toPx(), maxD), maxD)
            cos(ang) * d to sin(ang) * d
        }
    }
    val inner = popups.shuffled(random)
    inner.forEachIndexed { j, s -> s.spawnT = j * SPLASH_SPAWN_STAGGER }

    // edge Bs: most wait beyond the screen edge, a few peek in early
    val outCount = (SPLASH_B_COUNT - initialVisible).coerceAtLeast(0)
    val peekEvery = max(1, outCount / SPLASH_EDGE_PEEK_COUNT)
    val outer = List(outCount) { i ->
        val r = radius()
        val ang = off3 + (i + rand(0.1f, 0.9f)) * tau / outCount
        val peekIdx = i / peekEvery
        val peek = i % peekEvery == 0 && peekIdx < SPLASH_EDGE_PEEK_COUNT
        val reach = if (peek) edgeReach(ang, 0f) - r * SPLASH_EDGE_PEEK_INSET
                    else edgeReach(ang, -(r + SPLASH_OUTSIDE_MARGIN.toPx()))
        Slot(cos(ang) * reach, sin(ang) * reach, r).also {
            it.dur = SPLASH_OUTSIDE_PULL_DURATION
            it.spawnT = if (peek) SPLASH_EDGE_PEEK_START + peekIdx * SPLASH_EDGE_PEEK_STAGGER else -1f
        }
    }

    // schedule: first popup Bs one by one, then everything else in a cascade
    val earlyN = min(SPLASH_PULL_GROUPS.sum(), inner.size)
    var k = 0
    SPLASH_PULL_GROUPS.forEachIndexed { g, n ->
        repeat(n) { if (k < earlyN) inner[k++].pullT = SPLASH_PULL_GROUP_TIMES[g] }
    }
    (inner.drop(earlyN) + outer).shuffled(random).forEachIndexed { j, s ->
        s.pullT = SPLASH_PULL_GROUP_TIMES[SPLASH_PULL_GROUPS.size] + j * SPLASH_CASCADE_STAGGER
    }

    return (inner + outer).mapIndexed { i, s ->
        BallSpec(i % uriCount, s.r, s.x, s.y, s.spawnT, s.pullT, s.dur)
    }
}

// ============================================================================
// COMPOSE PREVIEWS
// ============================================================================

@Preview(name = "Splash Screen - 10 Album Arts (Light)", showBackground = true)
@Composable
private fun SplashComposeScreenTenItemsPreview() {
    val sampleUris = List(10) { Uri.parse("sample://album_art_$it") }
    MusicPlayerTheme(darkTheme = false) {
        SplashComposeScreen(albumArtUris = sampleUris)
    }
}

@Preview(name = "Splash Screen - 10 Album Arts (Dark)", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun SplashComposeScreenDarkPreview() {
    val sampleUris = List(10) { Uri.parse("sample://album_art_$it") }
    MusicPlayerTheme(darkTheme = true) {
        SplashComposeScreen(albumArtUris = sampleUris)
    }
}

@Preview(name = "Splash Screen - Clean Icon Only (<5 Album Arts)", showBackground = true)
@Composable
private fun SplashComposeScreenIconOnlyPreview() {
    MusicPlayerTheme {
        SplashComposeScreen(albumArtUris = emptyList())
    }
}
