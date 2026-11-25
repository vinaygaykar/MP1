package io.github.vinaygaykar.mp1.ui.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NowPlayingVisibilityController(
    private val coroutineScope: CoroutineScope,
    private val slideDistancePx: Float,
    private val nowPlayingOffsetY: Animatable<Float, AnimationVector1D>
) {
    /**
     * Slides the collapsed Now Playing view downward below the screen bounds.
     */
    fun hide() {
        coroutineScope.launch {
            nowPlayingOffsetY.animateTo(
                targetValue = slideDistancePx,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    /**
     * Slides the collapsed Now Playing view back up into its original screen position.
     */
    fun show() {
        coroutineScope.launch {
            nowPlayingOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }
}

@Composable
fun rememberNowPlayingVisibilityController(
    bottomPadding: Dp = 0.dp,
    onNowPlayingOffsetYChange: (Float) -> Unit = {}
): NowPlayingVisibilityController {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val nowPlayingOffsetY = remember { Animatable(0f) }

    LaunchedEffect(nowPlayingOffsetY.value) {
        onNowPlayingOffsetYChange(nowPlayingOffsetY.value)
    }

    val slideDistancePx = remember(density, bottomPadding) {
        with(density) { (72.dp + bottomPadding).toPx() }
    }

    return remember(coroutineScope, slideDistancePx, nowPlayingOffsetY) {
        NowPlayingVisibilityController(
            coroutineScope = coroutineScope,
            slideDistancePx = slideDistancePx,
            nowPlayingOffsetY = nowPlayingOffsetY
        )
    }
}
