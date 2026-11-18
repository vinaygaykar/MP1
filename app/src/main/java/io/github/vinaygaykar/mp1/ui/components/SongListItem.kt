package io.github.vinaygaykar.mp1.ui.components

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import io.github.vinaygaykar.mp1.domain.models.Song
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

data class SongOverlayData(
    val song: Song,
    val startOffset: Offset,
    val startSize: IntSize
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFastScrolling: Boolean = false,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: () -> Unit = {},
    onMoreClick: (Offset, IntSize) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val view = LocalView.current

    var artworkOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    var artworkSizePx by remember { mutableStateOf(IntSize.Zero) }

    val displayArtist = remember(song.artistsDisplayString, song.artist) {
        song.artistsDisplayString.ifBlank { song.artist.ifBlank { "Unknown Artist" } }
    }

    val imageRequest = remember(song.artworkUri) {
        if (song.artworkUri == null) null
        else {
            ImageRequest.Builder(context)
                .data(song.artworkUri)
                .size(144, 144)
                .precision(Precision.INEXACT)
                .crossfade(false)
                .build()
        }
    }

    ListItem(
        headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Clip) },
        supportingContent = { Text(displayArtist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .onGloballyPositioned { coordinates ->
                        artworkOffsetInRoot = coordinates.positionInRoot()
                        artworkSizePx = coordinates.size
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isFastScrolling || imageRequest == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val musicPlaceholder = rememberVectorPainter(Icons.Default.MusicNote)
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        error = musicPlaceholder,
                        placeholder = musicPlaceholder,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        trailingContent = {
            if (isSelectionMode) {
                IconButton(onClick = onLongClick) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Selected" else "Unselected",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        onMoreClick(artworkOffsetInRoot, artworkSizePx)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onLongClick()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                }
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailOverlay(
    song: Song,
    startOffset: Offset,
    startSize: IntSize,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    var isClosing by remember { mutableStateOf(false) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val dismissWithAnimation: () -> Unit = {
        if (!isClosing) {
            isClosing = true
            coroutineScope.launch {
                animProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                )
                onDismiss()
            }
        }
    }

    BackHandler {
        dismissWithAnimation()
    }

    Popup(
        onDismissRequest = dismissWithAnimation,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            clippingEnabled = false
        )
    ) {
        val screenWidthDp = configuration.screenWidthDp.dp
        val screenHeightDp = configuration.screenHeightDp.dp
        val screenWidthPx = with(density) { screenWidthDp.toPx() }
        val screenHeightPx = with(density) { screenHeightDp.toPx() }

        val p = animProgress.value

        val targetSizeDp = 180.dp
        val targetSizePx = with(density) { targetSizeDp.toPx() }
        val targetX = (screenWidthPx - targetSizePx) / 2f
        val targetY = with(density) { 72.dp.toPx() }

        val startSizePx = if (startSize.width > 0) startSize.width.toFloat() else with(density) { 48.dp.toPx() }
        val initialX = if (startOffset != Offset.Zero) startOffset.x else (screenWidthPx - startSizePx) / 2f
        val initialY = if (startOffset != Offset.Zero) startOffset.y else (screenHeightPx - startSizePx) / 2f

        val currentX = initialX + (targetX - initialX) * p
        val currentY = initialY + (targetY - initialY) * p
        val currentSizePx = startSizePx + (targetSizePx - startSizePx) * p
        val currentSizeDp = with(density) { currentSizePx.toDp() }

        Box(
            modifier = Modifier.size(screenWidthDp, screenHeightDp)
        ) {
            // Completely transparent backdrop layer (No dark color layer—keeps 100% pure blur of screen below)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = dismissWithAnimation
                    )
            )

            // Animated Circular Artwork (Remains circular throughout transition)
            Box(
                modifier = Modifier
                    .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
                    .size(currentSizeDp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                val musicPlaceholder = rememberVectorPainter(Icons.Default.MusicNote)
                AsyncImage(
                    model = song.artworkUri,
                    contentDescription = song.title,
                    error = musicPlaceholder,
                    placeholder = musicPlaceholder,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            // Reserved slot below top circular artwork for future content
            val topContentOffsetDp = with(density) { (targetY + targetSizePx + 24.dp.toPx()).toDp() }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = topContentOffsetDp)
                    .padding(horizontal = 24.dp)
                    .graphicsLayer {
                        alpha = p
                        translationY = (1f - p) * 40f
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
