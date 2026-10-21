package io.github.vinaygaykar.mp1.ui.nowplaying

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

@Composable
fun NowPlayingScreenBackground(
    state: NowPlayingScreenState,
    peekHeight: Dp,
    screenHeight: Dp,
    startPadding: Dp,
    endPadding: Dp,
    recordX: Dp,
    recordY: Dp,
    recordSize: Dp,
    cornerRadius: Dp,
    sheetShadowElevation: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val surfaceAlpha = 1f

    MaterialTheme(colorScheme = state.currentColorScheme) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(lerp(peekHeight, screenHeight, state.progress))
                .padding(start = startPadding, end = endPadding)
                .graphicsLayer {
                    scaleX = state.animatableBulge.value
                }
                .shadow(
                    elevation = sheetShadowElevation,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .clip(RoundedCornerShape(cornerRadius))
                .drawBehind {
                    val localRecordX = recordX - startPadding
                    val center = Offset(
                        x = (localRecordX + recordSize / 2).toPx(),
                        y = (recordY + recordSize / 2).toPx()
                    )
                    val pColor =
                        (state.bgColorPair?.first ?: state.displayedColorState ?: state.targetBgColor).copy(alpha = surfaceAlpha)
                    val cColor =
                        (state.bgColorPair?.second ?: state.displayedColorState ?: state.targetBgColor).copy(alpha = surfaceAlpha)
                    val maxRadius = size.width.coerceAtLeast(size.height) * 1.5f

                    if (state.isReverseRipple) {
                        drawRect(cColor)
                        drawCircle(
                            color = pColor,
                            radius = maxRadius * (1f - state.rippleProgress.value),
                            center = center
                        )
                    } else {
                        drawRect(pColor)
                        drawCircle(
                            color = cColor,
                            radius = maxRadius * state.rippleProgress.value,
                            center = center
                        )
                    }
                },
            color = Color.Transparent,
            shape = RoundedCornerShape(cornerRadius),
            tonalElevation = 8.dp
        ) {
            content()
        }
    }
}
