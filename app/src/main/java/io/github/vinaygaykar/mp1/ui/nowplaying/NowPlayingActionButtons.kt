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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.vinaygaykar.mp1.playback.PlaybackControls
import io.github.vinaygaykar.mp1.playback.PlaybackUiState
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme

@Composable
fun NowPlayingIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f),
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun NowPlayingRepeatButton(
    repeatMode: Int,
    onRepeatToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (repeatIcon, repeatText) = when (repeatMode) {
        1 -> Icons.Rounded.RepeatOne to "Song"
        2 -> Icons.Rounded.RepeatOn to "List"
        else -> Icons.Rounded.Repeat to "Off"
    }

    Surface(
        onClick = onRepeatToggle,
        modifier = modifier
            .width(110.dp)
            .height(48.dp),
        shape = CircleShape,
        color = if (repeatMode != 0) {
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f)
        },
        contentColor = if (repeatMode != 0) {
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
}

@Composable
fun NowPlayingCollapseHandle(
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    currentColorScheme: androidx.compose.material3.ColorScheme = MaterialTheme.colorScheme
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable { onCollapse() }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(currentColorScheme.onPrimaryContainer.copy(alpha = 0.4f))
        )
    }
}

@Composable
fun NowPlayingActionRow(
    repeatMode: Int,
    onLyricsClick: () -> Unit,
    onRepeatToggle: () -> Unit,
    onQueueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NowPlayingIconButton(
            icon = Icons.Rounded.Lyrics,
            contentDescription = "Lyrics",
            onClick = onLyricsClick
        )
        NowPlayingRepeatButton(
            repeatMode = repeatMode,
            onRepeatToggle = onRepeatToggle
        )
        NowPlayingIconButton(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            contentDescription = "Playing Queue",
            onClick = onQueueClick
        )
    }
}

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
    NowPlayingCollapseHandle(
        onCollapse = { state.collapse() },
        currentColorScheme = state.currentColorScheme,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 16.dp)
            .alpha(controlsAlpha)
    )

    if (isLandscape) {
        NowPlayingActionRow(
            repeatMode = playbackState.repeatMode,
            onLyricsClick = { state.showLyrics = true },
            onRepeatToggle = { playbackControls.onRepeatToggle() },
            onQueueClick = { state.showQueue = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 20.dp)
                .alpha(controlsAlpha)
        )
    } else {
        NowPlayingIconButton(
            icon = Icons.Rounded.Lyrics,
            contentDescription = "Lyrics",
            onClick = { state.showLyrics = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 48.dp)
                .alpha(controlsAlpha)
        )

        NowPlayingRepeatButton(
            repeatMode = playbackState.repeatMode,
            onRepeatToggle = { playbackControls.onRepeatToggle() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(controlsAlpha)
        )

        NowPlayingIconButton(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            contentDescription = "Playing Queue",
            onClick = { state.showQueue = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 48.dp)
                .alpha(controlsAlpha)
        )
    }
}

@Preview(showBackground = true, name = "Action Row - Light")
@Composable
fun NowPlayingActionRowLightPreview() {
    MusicPlayerTheme(darkTheme = false) {
        Surface {
            NowPlayingActionRow(
                repeatMode = 1,
                onLyricsClick = {},
                onRepeatToggle = {},
                onQueueClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Action Row - Dark")
@Composable
fun NowPlayingActionRowDarkPreview() {
    MusicPlayerTheme(darkTheme = true) {
        Surface {
            NowPlayingActionRow(
                repeatMode = 1,
                onLyricsClick = {},
                onRepeatToggle = {},
                onQueueClick = {}
            )
        }
    }
}


