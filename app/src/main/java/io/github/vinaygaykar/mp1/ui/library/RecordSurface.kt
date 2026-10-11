package io.github.vinaygaykar.mp1.ui.library

import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme

@Composable
fun RecordSurface(
    artworkUri: Uri?,
    recordSize: Dp,
    artworkSize: Dp,
    rotation: Float,
    overlayAlpha: Float,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.Transparent,
    isScrubbing: Boolean = false,
    isDraggingArtwork: Boolean = false
) {
    val blendMode = if (lineColor == Color.Transparent) BlendMode.Clear else BlendMode.SrcOver

    val outerCircleScale by animateFloatAsState(
        targetValue = if (isScrubbing) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "outer_circle_scrub_scale"
    )

    val artworkScale by animateFloatAsState(
        targetValue = if (isDraggingArtwork) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "artwork_drag_scale"
    )

    val containerSize = recordSize * 1.15f

    Box(
        modifier = modifier
            .size(containerSize)
            .graphicsLayer {
                rotationZ = rotation % 360f
                compositingStrategy = CompositingStrategy.Offscreen
            },
        contentAlignment = Alignment.Center
    ) {
        // Outer Vinyl Circle & Canvas Lines (scales up to 15% when scrubbing)
        Box(
            modifier = Modifier
                .size(recordSize)
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = outerCircleScale
                    scaleY = outerCircleScale
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
        ) {
            // Rotating indicator line segments behind album art
            Canvas(modifier = Modifier.fillMaxSize()) {
                val inset = 8.dp.toPx()
                val strokeWidthPx = 4.dp.toPx()
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val artworkRadius = (artworkSize.toPx() * artworkScale / outerCircleScale) / 2f

                val leftLineStart = inset
                val leftLineEnd = centerX - artworkRadius - inset

                val rightLineStart = centerX + artworkRadius + inset
                val rightLineEnd = size.width - inset

                if (leftLineEnd > leftLineStart) {
                    // Left line segment
                    drawLine(
                        color = lineColor,
                        start = Offset(leftLineStart, centerY),
                        end = Offset(leftLineEnd, centerY),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round,
                        blendMode = blendMode
                    )
                }

                if (rightLineEnd > rightLineStart) {
                    // Right line segment
                    drawLine(
                        color = lineColor,
                        start = Offset(rightLineStart, centerY),
                        end = Offset(rightLineEnd, centerY),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round,
                        blendMode = blendMode
                    )
                }
            }
        }

        // Album Artwork (scales up to 15% when dragging artwork)
        Box(
            modifier = Modifier
                .size(artworkSize)
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = artworkScale
                    scaleY = artworkScale
                }
        ) {
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            // Pause Overlay
            if (overlayAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = overlayAlpha))
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Record Surface - Playing")
@Composable
fun RecordSurfacePlayingPreview() {
    MusicPlayerTheme {
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            RecordSurface(
                artworkUri = null,
                recordSize = 260.dp,
                artworkSize = 200.dp,
                rotation = 45f,
                overlayAlpha = 0f
            )
        }
    }
}

@Preview(showBackground = true, name = "Record Surface - Paused")
@Composable
fun RecordSurfacePausedPreview() {
    MusicPlayerTheme {
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            RecordSurface(
                artworkUri = null,
                recordSize = 260.dp,
                artworkSize = 200.dp,
                rotation = 0f,
                overlayAlpha = 0f
            )
        }
    }
}
