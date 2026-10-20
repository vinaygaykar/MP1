package io.github.vinaygaykar.mp1.ui.components

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import kotlin.math.roundToInt

data class FlyingArtworkItem(
    val id: Long = System.currentTimeMillis() + (0..1000000).random(),
    val artworkUri: Uri?,
    val startOffset: Offset,
    val startSize: IntSize
)

@Composable
fun FlyingArtworkOverlay(
    item: FlyingArtworkItem,
    rootOffset: Offset,
    targetOffsetInLocal: Offset,
    onAnimationComplete: () -> Unit
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(item.id) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 420,
                easing = FastOutSlowInEasing
            )
        )
        onAnimationComplete()
    }

    val p = animProgress.value

    val startX = item.startOffset.x - rootOffset.x
    val startY = item.startOffset.y - rootOffset.y
    val targetX = targetOffsetInLocal.x
    val targetY = targetOffsetInLocal.y

    val currentX = lerp(startX, targetX, p)
    val linearY = lerp(startY, targetY, p)
    val arcOffset = -30f * (1f - p) * p
    val currentY = linearY + arcOffset

    val sizeDp = 56.dp
    val currentAlpha = if (p > 0.5f) ((1f - p) / 0.5f).coerceIn(0f, 1f) else 1f

    Box(
        modifier = Modifier
            .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
            .size(sizeDp)
            .zIndex(999f)
            .graphicsLayer {
                alpha = currentAlpha
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        val musicPlaceholder = rememberVectorPainter(Icons.Default.MusicNote)
        AsyncImage(
            model = item.artworkUri,
            contentDescription = null,
            error = musicPlaceholder,
            placeholder = musicPlaceholder,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
