package com.prelude.iptv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%d:%02d", minutes, seconds)
}

enum class AspectMode {
    FIT,
    FILL,
    FORCE_4_3,
    FORCE_16_9;

    fun next(): AspectMode = entries[(ordinal + 1) % entries.size]
}

enum class PlayerMenu { AUDIO, SUBTITLES, ASPECT, QUALITY, SPEED, SLEEP }

internal val SUBTITLE_SIZES = listOf(70, 85, 100, 115, 130, 150, 180)

internal val SUBTITLE_BACKGROUNDS = listOf("shadow", "box", "none")

internal val SPEED_OPTIONS = listOf(1f, 0.75f, 1.25f, 1.5f, 2f)

internal val SLEEP_OPTIONS = listOf(0, 15, 30, 45, 60, 90)

@Composable
fun PlayerExtraAction(label: String, onClick: () -> Unit) {
    PlayerActionButton(label = label, onClick = onClick)
}

@Composable
internal fun PlayerActionButton(
    label: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    leading: String? = null,
    primary: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val foreground = if (focused || primary) Color.Black else Color.White
    Row(
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .graphicsLayer {
                val scale = if (focused) 1.06f else 1f
                scaleX = scale
                scaleY = scale
                shadowElevation = if (focused) 18.dp.toPx() else 0f
            }
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Color.White else Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(8.dp),
            )
            .background(
                when {
                    focused -> Color.White
                    primary -> Color.White.copy(alpha = 0.94f)
                    else -> Color.Black.copy(alpha = 0.72f)
                }
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
    ) {
        if (!leading.isNullOrBlank()) {
            Text(
                text = leading,
                color = foreground,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            color = foreground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}
