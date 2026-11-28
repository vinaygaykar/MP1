package io.github.vinaygaykar.mp1.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.artistSharedElement(
    key: String,
    sharedTransitionScope: SharedTransitionScope? = LocalSharedTransitionScope.current,
    animatedVisibilityScope: AnimatedVisibilityScope? = LocalAnimatedVisibilityScope.current
): Modifier {
    return if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            this@artistSharedElement.sharedElement(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else {
        this
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.albumSharedElement(
    key: String,
    sharedTransitionScope: SharedTransitionScope? = LocalSharedTransitionScope.current,
    animatedVisibilityScope: AnimatedVisibilityScope? = LocalAnimatedVisibilityScope.current
): Modifier {
    return if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            this@albumSharedElement.sharedElement(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else {
        this
    }
}
