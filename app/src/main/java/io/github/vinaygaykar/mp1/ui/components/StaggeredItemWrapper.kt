package io.github.vinaygaykar.mp1.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Wraps an item in a list or grid to perform an entrance animation:
 * 1. Staggered slide-in ONLY for initial visible items (index <= 10) when page first opens.
 * 2. Instant display (animProgress = 1.0f) for all scrolled items and when scrolling back up.
 * 3. Waits until splash animation completion event (!isSplashActive) before starting initial entrance animation.
 */
@Composable
fun StaggeredItemWrapper(
    index: Int,
    modifier: Modifier = Modifier,
    categoryKey: Any? = null,
    detailProgress: Float = 1f,
    isSplashActive: Boolean = false,
    isCategoryEntranceComplete: Boolean = false,
    slideDistance: Dp = 100.dp,
    content: @Composable () -> Unit,
) {
    val isInitialItem = index <= 10
    val shouldAnimate = isInitialItem && !isCategoryEntranceComplete
    val animProgress = remember(categoryKey, isCategoryEntranceComplete) {
        Animatable(if (shouldAnimate) 0f else 1f)
    }

    LaunchedEffect(categoryKey, isSplashActive, detailProgress > 0.5f, isCategoryEntranceComplete) {
        if (shouldAnimate && (animProgress.value < 1f)) {
            if (!isSplashActive && (detailProgress > 0.5f)) {
                val delayMs = index * 32
                delay(delayMs.milliseconds)
                animProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = EaseOutCubic,
                    ),
                )
            }
        }
    }

    val progress = animProgress.value
    val density = LocalDensity.current
    val slideDistancePx = with(density) { slideDistance.toPx() }

    Box(
        modifier = modifier.graphicsLayer {
            translationY = (1f - progress) * slideDistancePx
            val scale = 0.90f + (0.10f * progress)
            scaleX = scale
            scaleY = scale
            alpha = progress.coerceIn(0f, 1f)
        },
    ) {
        content()
    }
}
