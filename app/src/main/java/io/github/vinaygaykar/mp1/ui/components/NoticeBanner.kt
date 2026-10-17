package io.github.vinaygaykar.mp1.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme

@Composable
fun NoticeBanner(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector = Icons.Default.Info,
    onClick: (() -> Unit)? = null,
    showBottomDivider: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = if (onClick != null) {
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        } else {
            modifier.fillMaxWidth()
        },
        shape = RectangleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = contentColor,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    trailingContent()
                }
            }
            if (showBottomDivider) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth(0.2f)
                        .align(Alignment.CenterHorizontally),
                    thickness = 1.dp,
                    color = contentColor.copy(alpha = 0.2f),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoticeBannerPreview() {
    MusicPlayerTheme {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            NoticeBanner(
                title = "Any changes on this screen will stop current song and clear your current playing queue.",
                showBottomDivider = true,
            )

            NoticeBanner(
                title = "Library scan in progress...",
                subtitle = "Scanning /storage/emulated/0/Music",
                trailingContent = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                },
            )
        }
    }
}
