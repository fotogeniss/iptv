package com.prelude.iptv.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.player.PlaybackEngine
import com.prelude.iptv.R
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.TvDialogTextButton
import com.prelude.iptv.ui.requestFocusWithRetry

@Composable
internal fun PlayerTrackMenu(
    title: String,
    options: List<PlaybackEngine.TrackOption>,
    allowDisable: Boolean,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
    disableLabel: String? = null,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(title) { firstFocus.requestFocusWithRetry() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IptvColors.SurfaceRaised,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (options.isEmpty()) {
                    Text(
                        stringResource(R.string.player_no_stream_options),
                        color = IptvColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
                options.forEachIndexed { index, option ->
                    PlayerActionButton(
                        label = (if (option.selected) "● " else "") + option.label,
                        focusRequester = if (index == 0) firstFocus else null,
                        onClick = { onSelect(option.id) }
                    )
                }
                if (allowDisable) {
                    PlayerActionButton(
                        label = disableLabel ?: stringResource(R.string.player_no_subtitles),
                        focusRequester = if (options.isEmpty()) firstFocus else null,
                        onClick = { onSelect(null) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TvDialogTextButton(label = stringResource(R.string.player_close), color = Color.White, onClick = onDismiss)
        }
    )
}

@Composable
internal fun PlayerChoiceMenu(
    title: String,
    options: List<Pair<String, Boolean>>,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(title) { firstFocus.requestFocusWithRetry() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IptvColors.SurfaceRaised,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEachIndexed { index, (label, selected) ->
                    PlayerActionButton(
                        label = (if (selected) "● " else "") + label,
                        focusRequester = if (index == 0) firstFocus else null,
                        onClick = { onSelect(index) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TvDialogTextButton(label = stringResource(R.string.player_close), color = Color.White, onClick = onDismiss)
        }
    )
}
