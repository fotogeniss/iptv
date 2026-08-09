package com.prelude.iptv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.ui.IptvColors

/**
 * Βήμα αναζήτησης όταν η μπάρα προόδου έχει πραγματικά το focus.
 * Δεν υπάρχουν ανεξάρτητα κουμπιά ±10″ και το root του player δεν κάνει seek.
 */
private const val SCRUB_STEP_MS = 30_000L

/**
 * Το κοινό TV chrome. Η διαδρομή focus είναι ρητή και σταθερή:
 *
 * μπάρα προόδου ↕ αναπαραγωγή → υπότιτλοι/ήχος → εικόνα → ποιότητα → ταχύτητα → ύπνος.
 *
 * Έτσι η χωρική αναζήτηση του Compose δεν χρειάζεται να μαντέψει ποιο control είναι
 * «πιο κοντά» όταν αλλάζει το μήκος ενός τίτλου ή εμφανίζεται προαιρετικό κουμπί.
 */
@Composable
internal fun PlayerControlsBar(
    title: String,
    subtitle: String,
    qualityLabel: String,
    playing: Boolean,
    seekable: Boolean,
    positionMs: Long,
    durationMs: Long,
    aspectLabel: String,
    speedLabel: String,
    sleepRemainingMs: Long,
    hasMultipleQualities: Boolean,
    channelStepAvailable: Boolean,
    fetchingSubtitles: Boolean,
    progressFocus: FocusRequester,
    playFocus: FocusRequester,
    tracksFocus: FocusRequester,
    aspectFocus: FocusRequester,
    qualityFocus: FocusRequester,
    speedFocus: FocusRequester,
    sleepFocus: FocusRequester,
    onSeekBy: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onOpenMenu: (PlayerMenu) -> Unit,
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    extraActions: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val progressAvailable = seekable && durationMs > 0L
    val afterAspect = when {
        hasMultipleQualities -> qualityFocus
        seekable -> speedFocus
        else -> sleepFocus
    }
    val beforeSleep = when {
        seekable -> speedFocus
        hasMultipleQualities -> qualityFocus
        else -> aspectFocus
    }

    Box(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.22f to Color.Black.copy(alpha = 0.28f),
                    1f to Color.Black.copy(alpha = 0.94f),
                )
            )
            .padding(start = 48.dp, end = 48.dp, top = 58.dp, bottom = 28.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (qualityLabel.isNotBlank()) {
                    Text(
                        text = qualityLabel,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
            }

            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = IptvColors.TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (progressAvailable) {
                Spacer(Modifier.height(13.dp))
                var scrubFocused by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusProperties { down = playFocus }
                        .focusRequester(progressFocus)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (scrubFocused) Color.White.copy(alpha = 0.13f)
                            else Color.Transparent
                        )
                        .onFocusChanged { scrubFocused = it.isFocused }
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.DirectionLeft -> {
                                    onSeekBy(-SCRUB_STEP_MS)
                                    true
                                }
                                Key.DirectionRight -> {
                                    onSeekBy(SCRUB_STEP_MS)
                                    true
                                }
                                else -> false
                            }
                        }
                        .focusable()
                        .padding(horizontal = 8.dp)
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatDuration(positionMs),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    val fraction = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                    Canvas(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                            .height(8.dp)
                    ) {
                        val lineHeight = (if (scrubFocused) 3.dp else 2.dp).toPx()
                        val lineTop = (size.height - lineHeight) / 2f
                        val lineRadius = CornerRadius(lineHeight / 2f, lineHeight / 2f)
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.28f),
                            topLeft = Offset(0f, lineTop),
                            size = Size(size.width, lineHeight),
                            cornerRadius = lineRadius,
                        )
                        val playedWidth = size.width * fraction
                        if (playedWidth > 0f) {
                            drawRoundRect(
                                color = IptvColors.Primary,
                                topLeft = Offset(0f, lineTop),
                                size = Size(playedWidth, lineHeight),
                                cornerRadius = lineRadius,
                            )
                        }
                        val thumbRadius = (if (scrubFocused) 4.dp else 3.dp).toPx()
                        val maxThumbX = (size.width - thumbRadius).coerceAtLeast(thumbRadius)
                        drawCircle(
                            color = Color.White,
                            radius = thumbRadius,
                            center = Offset(
                                x = playedWidth.coerceIn(thumbRadius, maxThumbX),
                                y = size.height / 2f,
                            ),
                        )
                    }
                    Text(
                        text = formatDuration(durationMs),
                        color = IptvColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(13.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                PlayerActionButton(
                    label = stringResource(if (playing) R.string.player_pause else R.string.player_play),
                    leading = if (playing) "Ⅱ" else "▶",
                    primary = true,
                    focusRequester = playFocus,
                    modifier = Modifier.focusProperties {
                        if (progressAvailable) up = progressFocus
                        right = tracksFocus
                    },
                    onClick = onTogglePlay,
                )
                PlayerActionButton(
                    label = stringResource(R.string.player_subtitles_audio),
                    leading = "CC",
                    focusRequester = tracksFocus,
                    modifier = Modifier.focusProperties {
                        left = playFocus
                        right = aspectFocus
                        if (progressAvailable) up = progressFocus
                    },
                    onClick = { onOpenMenu(PlayerMenu.SUBTITLES) },
                )
                PlayerActionButton(
                    label = stringResource(R.string.player_image_value, aspectLabel),
                    leading = "▭",
                    focusRequester = aspectFocus,
                    modifier = Modifier.focusProperties {
                        left = tracksFocus
                        right = afterAspect
                        if (progressAvailable) up = progressFocus
                    },
                    onClick = { onOpenMenu(PlayerMenu.ASPECT) },
                )
                if (hasMultipleQualities) {
                    PlayerActionButton(
                        label = stringResource(R.string.player_quality),
                        leading = "HD",
                        focusRequester = qualityFocus,
                        modifier = Modifier.focusProperties {
                            left = aspectFocus
                            right = if (seekable) speedFocus else sleepFocus
                            if (progressAvailable) up = progressFocus
                        },
                        onClick = { onOpenMenu(PlayerMenu.QUALITY) },
                    )
                }
                if (seekable) {
                    PlayerActionButton(
                        label = stringResource(R.string.player_speed_value, speedLabel),
                        leading = "1×",
                        focusRequester = speedFocus,
                        modifier = Modifier.focusProperties {
                            left = if (hasMultipleQualities) qualityFocus else aspectFocus
                            right = sleepFocus
                            if (progressAvailable) up = progressFocus
                        },
                        onClick = { onOpenMenu(PlayerMenu.SPEED) },
                    )
                }
                PlayerActionButton(
                    label = if (sleepRemainingMs > 0) {
                        stringResource(
                            R.string.player_sleep_value,
                            formatDuration(sleepRemainingMs),
                        )
                    } else {
                        stringResource(R.string.player_sleep)
                    },
                    leading = "◔",
                    focusRequester = sleepFocus,
                    modifier = Modifier.focusProperties {
                        left = beforeSleep
                        if (progressAvailable) up = progressFocus
                    },
                    onClick = { onOpenMenu(PlayerMenu.SLEEP) },
                )
                if (isFavorite != null && onToggleFavorite != null) {
                    PlayerActionButton(
                        label = stringResource(
                            if (isFavorite) R.string.player_favorite_on else R.string.player_favorite_off
                        ),
                        onClick = onToggleFavorite,
                    )
                }
                extraActions?.invoke()
            }

            Spacer(Modifier.height(8.dp))
            val timelineHint = stringResource(R.string.player_hint_timeline)
            val channelHint = stringResource(R.string.player_hint_channel)
            val selectHint = stringResource(R.string.player_hint_select)
            val subtitleHint = stringResource(R.string.player_searching_subtitles_suffix)
            Text(
                text = buildString {
                    if (seekable) append(timelineHint)
                    if (channelStepAvailable) append(channelHint)
                    append(selectHint)
                    if (fetchingSubtitles) append(subtitleHint)
                },
                color = IptvColors.TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
