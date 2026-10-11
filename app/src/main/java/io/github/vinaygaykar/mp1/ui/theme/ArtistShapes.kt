package io.github.vinaygaykar.mp1.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

/**
 * Custom Compose [Shape] wrapping a [RoundedPolygon] from `androidx.graphics.shapes`.
 * Centers the polygon inside the target [Size] bounds and scales it uniformly.
 */
class RoundedPolygonShape(polygon: RoundedPolygon) : Shape {
    private val basePath: Path = polygon.toPath().asComposePath()
    private val bounds = basePath.getBounds()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        if (size.width <= 0f || size.height <= 0f || bounds.width <= 0f || bounds.height <= 0f) {
            return Outline.Generic(Path())
        }
        val scaleX = size.width / bounds.width
        val scaleY = size.height / bounds.height
        val scale = minOf(scaleX, scaleY)

        val scaledPath = Path().apply {
            addPath(basePath)
            val matrix = Matrix().apply {
                translate(size.width / 2f, size.height / 2f)
                scale(scale, scale)
                translate(-bounds.center.x, -bounds.center.y)
            }
            transform(matrix)
        }
        return Outline.Generic(scaledPath)
    }
}

/**
 * Custom Compose [Shape] wrapping a morphing transition between two [RoundedPolygon]s.
 * Centers the morph path inside the target [Size] bounds and scales it uniformly.
 */
class MorphPolygonShape(
    private val morph: Morph,
    private val progress: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        if (size.width <= 0f || size.height <= 0f) {
            return Outline.Generic(Path())
        }
        val composePath = morph.toPath(progress).asComposePath()
        val bounds = composePath.getBounds()
        if (bounds.width <= 0f || bounds.height <= 0f) {
            return Outline.Generic(Path())
        }
        val scaleX = size.width / bounds.width
        val scaleY = size.height / bounds.height
        val scale = minOf(scaleX, scaleY)

        val scaledPath = Path().apply {
            addPath(composePath)
            val matrix = Matrix().apply {
                translate(size.width / 2f, size.height / 2f)
                scale(scale, scale)
                translate(-bounds.center.x, -bounds.center.y)
            }
            transform(matrix)
        }
        return Outline.Generic(scaledPath)
    }
}

object ArtistShapes {
    val CloverPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 4,
        innerRadius = 0.60f,
        rounding = CornerRounding(radius = 0.35f, smoothing = 0.5f)
    )

    val HexagonPolygon = RoundedPolygon(
        numVertices = 6,
        rounding = CornerRounding(radius = 0.25f, smoothing = 0.6f)
    )

    val CookiePolygon = RoundedPolygon.star(
        numVerticesPerRadius = 6,
        innerRadius = 0.80f,
        rounding = CornerRounding(radius = 0.20f, smoothing = 0.8f)
    )

    val ShieldPolygon = RoundedPolygon(
        numVertices = 3,
        rounding = CornerRounding(radius = 0.32f, smoothing = 0.5f)
    )

    val BloomPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 8,
        innerRadius = 0.82f,
        rounding = CornerRounding(radius = 0.18f, smoothing = 0.6f)
    )

    val SquirclePolygon = RoundedPolygon(
        numVertices = 4,
        rounding = CornerRounding(radius = 0.40f, smoothing = 0.8f)
    )

    val artistPolygonList = listOf(
        CloverPolygon,
        HexagonPolygon,
        CookiePolygon,
        ShieldPolygon,
        BloomPolygon,
        SquirclePolygon
    )

    // 1. Clover (4-lobed star)
    val Clover: Shape = RoundedPolygonShape(CloverPolygon)

    // 2. Rounded Hexagon
    val Hexagon: Shape = RoundedPolygonShape(HexagonPolygon)

    // 3. Scalloped Cookie
    val Cookie: Shape = RoundedPolygonShape(CookiePolygon)

    // 4. Rounded Shield / Triangle
    val Shield: Shape = RoundedPolygonShape(ShieldPolygon)

    // 5. 8-Point Bloom / Starburst
    val Bloom: Shape = RoundedPolygonShape(BloomPolygon)

    // 6. Smooth Squircle
    val Squircle: Shape = RoundedPolygonShape(SquirclePolygon)

    /**
     * List of expressively shaped polygon profiles for artists.
     * Note: Pill shape is intentionally omitted.
     */
    val artistShapeList = listOf(
        Clover,
        Hexagon,
        Cookie,
        Shield,
        Bloom,
        Squircle
    )

    /**
     * Deterministically returns the base [RoundedPolygon] for a given artist.
     */
    fun getArtistPolygon(artistId: String, artistName: String = ""): RoundedPolygon {
        val key = artistId.ifEmpty { artistName }
        if (key.isEmpty()) return CloverPolygon
        val index = abs(key.hashCode()) % artistPolygonList.size
        return artistPolygonList[index]
    }

    /**
     * Deterministically returns a consistent [Shape] for a given artist based on their ID or name.
     */
    fun getArtistShape(artistId: String, artistName: String = ""): Shape {
        val key = artistId.ifEmpty { artistName }
        if (key.isEmpty()) return Clover
        val index = abs(key.hashCode()) % artistShapeList.size
        return artistShapeList[index]
    }
}

/**
 * Helper to check if an artist matches the current playing track's artist or album string.
 * Uses exact token matching (case-insensitive) rather than substring matching to prevent
 * false positives (e.g. artist "AC" matching when playing "2Pac").
 */
fun isArtistMatchingPlayingTrack(
    artistName: String,
    playingArtistString: String,
    playingAlbumString: String
): Boolean {
    if (artistName.isBlank()) return false
    val cleanArtistName = artistName.trim()
    if (cleanArtistName.isBlank()) return false

    val cleanPlayingArtist = playingArtistString.trim()
    val cleanPlayingAlbum = playingAlbumString.trim()

    // 1. Exact full match (case-insensitive)
    if (cleanPlayingArtist.equals(cleanArtistName, ignoreCase = true) ||
        cleanPlayingAlbum.equals(cleanArtistName, ignoreCase = true)) {
        return true
    }

    // 2. Tokenized match for multi-artist tracks (split by commas, ampersands, feat/ft/with/vs)
    val delimiters = Regex("[,&]|\\b(feat|ft|featuring|with|vs)\\b\\.?", RegexOption.IGNORE_CASE)

    val artistTokens = cleanPlayingArtist.split(delimiters)
    if (artistTokens.any { it.trim().equals(cleanArtistName, ignoreCase = true) }) {
        return true
    }

    val albumTokens = cleanPlayingAlbum.split(delimiters)
    return albumTokens.any { it.trim().equals(cleanArtistName, ignoreCase = true) }
}

@Composable
fun rememberArtistShape(artistId: String, artistName: String = ""): Shape {
    return remember(artistId, artistName) {
        ArtistShapes.getArtistShape(artistId, artistName)
    }
}

/**
 * Dynamically morphs between multiple random polygons when [isMorphing] is true.
 * Returns the default static [Shape] when [isMorphing] is false.
 */
@Composable
fun rememberMorphingArtistShape(
    artistId: String,
    artistName: String = "",
    isMorphing: Boolean
): Shape {
    val defaultShape = rememberArtistShape(artistId, artistName)
    if (!isMorphing) return defaultShape

    val defaultPolygon = remember(artistId, artistName) {
        ArtistShapes.getArtistPolygon(artistId, artistName)
    }

    var fromPolygon by remember { mutableStateOf(defaultPolygon) }
    var toPolygon by remember {
        mutableStateOf(
            ArtistShapes.artistPolygonList
                .filter { it != defaultPolygon }
                .random()
        )
    }

    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(isMorphing) {
        if (isMorphing) {
            while (isActive) {
                animProgress.snapTo(0f)
                animProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 1800,
                        easing = LinearOutSlowInEasing
                    )
                )
                delay(200)
                fromPolygon = toPolygon
                val available = ArtistShapes.artistPolygonList.filter { it != fromPolygon }
                toPolygon = available.random()
            }
        }
    }

    val morph = remember(fromPolygon, toPolygon) {
        Morph(fromPolygon, toPolygon)
    }

    return MorphPolygonShape(morph, animProgress.value)
}
