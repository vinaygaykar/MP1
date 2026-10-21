package io.github.vinaygaykar.mp1.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.RepeatOn
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vinaygaykar.mp1.playback.PlaybackControls
import io.github.vinaygaykar.mp1.playback.PlaybackUiState

@Composable
fun BoxScope.NowPlayingActionButtons(
    state: NowPlayingScreenState,
    playbackState: PlaybackUiState,
    playbackControls: PlaybackControls,
    isLandscape: Boolean
) {
    if (state.progress <= 0.8f) return

    val controlsAlpha = ((state.progress - 0.8f) / 0.2f).coerceIn(0f, 1f)

    // Top Collapse Handle
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 16.dp)
            .alpha(controlsAlpha)
            .clip(CircleShape)
            .clickable {
                state.collapse()
            }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(state.currentColorScheme.onPrimaryContainer.copy(alpha = 0.4f))
        )
    }

    // Action Buttons (Lyrics, Repeat, Queue)
    val (repeatIcon, repeatText) = when (playbackState.repeatMode) {
        1 -> Icons.Rounded.RepeatOne to "Song"
        2 -> Icons.Rounded.RepeatOn to "List"
        else -> Icons.Rounded.Repeat to "Off"
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 20.dp)
                .alpha(controlsAlpha),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = { state.showLyrics = true },
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = "Lyrics",
                    modifier = Modifier.size(24.dp)
                )
            }

            Surface(
                onClick = { playbackControls.onRepeatToggle() },
                modifier = Modifier
                    .width(110.dp)
                    .height(48.dp),
                shape = CircleShape,
                color = if (playbackState.repeatMode != 0) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f)
                },
                contentColor = if (playbackState.repeatMode != 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = repeatText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = repeatText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            FilledIconButton(
                onClick = { state.showQueue = true },
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "Playing Queue",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    } else {
        FilledIconButton(
            onClick = { state.showLyrics = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 48.dp)
                .alpha(controlsAlpha)
                .size(48.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f),
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Lyrics,
                contentDescription = "Lyrics",
                modifier = Modifier.size(24.dp)
            )
        }

        Surface(
            onClick = { playbackControls.onRepeatToggle() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(controlsAlpha)
                .width(110.dp)
                .height(48.dp),
            shape = CircleShape,
            color = if (playbackState.repeatMode != 0) {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f)
            },
            contentColor = if (playbackState.repeatMode != 0) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = repeatIcon,
                    contentDescription = repeatText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = repeatText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        FilledIconButton(
            onClick = { state.showQueue = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 48.dp)
                .alpha(controlsAlpha)
                .size(48.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f),
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = "Playing Queue",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
