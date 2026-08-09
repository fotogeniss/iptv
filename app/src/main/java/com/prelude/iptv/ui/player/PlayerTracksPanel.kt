package com.prelude.iptv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prelude.iptv.R
import com.prelude.iptv.player.PlaybackEngine
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.requestFocusWithRetry
import com.prelude.iptv.ui.localization.localizedSubtitleBackground

internal enum class PlayerTracksTab { SUBTITLES, AUDIO }

/**
 * Το κοινό panel που ανοίγει το CC. Ο player και τα controls μένουν ανέγγιχτα·
 * μόνο αυτό το layer ζωγραφίζεται από πάνω. Υπότιτλοι και ήχος είναι tabs του
 * ίδιου panel επειδή είναι οι δύο ομάδες tracks της ίδιας ροής.
 */
@Composable
internal fun PlayerTracksPanel(
    initialTab: PlayerTracksTab,
    audioTracks: List<PlaybackEngine.TrackOption>,
    subtitleTracks: List<PlaybackEngine.TrackOption>,
    subtitleSize: Int,
    subtitleBackground: String,
    subtitleBold: Boolean,
    subtitleQuery: String,
    searchSubtitles: (suspend (String) -> List<ExternalSubtitle>)?,
    onAutoFetchSubtitles: (() -> Unit)?,
    onSelectAudio: (String) -> Unit,
    onSelectSubtitle: (String?) -> Unit,
    onSubtitleSize: (Int) -> Unit,
    onSubtitleBackground: (String) -> Unit,
    onSubtitleBold: (Boolean) -> Unit,
    onSubtitleChosen: (ExternalSubtitle) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    var manualSearchOpen by remember { mutableStateOf(false) }
    val panelScrollState = rememberScrollState()
    val subtitleTabFocus = remember { FocusRequester() }
    val audioTabFocus = remember { FocusRequester() }
    LaunchedEffect(initialTab) {
        if (initialTab == PlayerTracksTab.AUDIO) {
            audioTabFocus.requestFocusWithRetry()
        } else {
            subtitleTabFocus.requestFocusWithRetry()
        }
    }
    LaunchedEffect(selectedTab) {
        panelScrollState.scrollTo(0)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = .68f))
                // Το Dialog είναι edge-to-edge. Χωρίς system insets, το κάτω
                // μέρος του panel μετριόταν πίσω από τη navigation bar και η
                // τελευταία ρύθμιση φαινόταν κομμένη.
                .navigationBarsPadding()
                .imePadding()
                .testTag("player-tracks-scrim")
                .clickable(onClick = onDismiss),
        ) {
            val landscape = maxWidth > maxHeight
            val landscapePanelWidth = PlayerTracksPanelLayoutPolicy
                .landscapePanelWidthDp(maxWidth.value)
                .dp
            Column(
                Modifier
                    .align(if (landscape) Alignment.TopEnd else Alignment.BottomCenter)
                    .then(
                        if (landscape) {
                            Modifier
                                // Το κενό είναι ΕΞΩ από το πραγματικό panel. Έτσι το
                                // κουμπί κλεισίματος δεν φεύγει ποτέ πέρα από τη δεξιά
                                // άκρη, ακόμη και σε κινητά με μεγάλη density/cutout.
                                .padding(start = 16.dp, top = 14.dp, end = 24.dp, bottom = 20.dp)
                                .width(landscapePanelWidth)
                                .fillMaxHeight()
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(.88f)
                                // Some OEM dialog windows report a zero navigation-bar
                                // inset even though 3-button navigation is visible. Keep
                                // a physical safety gap so the last row never sits behind it.
                                .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 48.dp)
                        }
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xF509090C))
                    .border(1.dp, Color.White.copy(alpha = .09f), RoundedCornerShape(24.dp))
                    .testTag("player-tracks-panel")
                    // Καταναλώνει το πάτημα ώστε μόνο το εξωτερικό μαύρο layer να κλείνει.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color.Black)
                            .padding(4.dp),
                    ) {
                        TracksTabButton(
                            title = stringResource(R.string.player_subtitles),
                            selected = selectedTab == PlayerTracksTab.SUBTITLES,
                            modifier = Modifier.weight(1f).focusRequester(subtitleTabFocus),
                            onClick = { selectedTab = PlayerTracksTab.SUBTITLES },
                        )
                        TracksTabButton(
                            title = stringResource(R.string.player_audio),
                            selected = selectedTab == PlayerTracksTab.AUDIO,
                            modifier = Modifier.weight(1f).focusRequester(audioTabFocus),
                            onClick = { selectedTab = PlayerTracksTab.AUDIO },
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(IptvColors.Surface)
                            .testTag("player-tracks-close")
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Close,
                            stringResource(R.string.player_close),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(panelScrollState),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    if (selectedTab == PlayerTracksTab.SUBTITLES) {
                        SubtitleTracksContent(
                            tracks = subtitleTracks,
                            size = subtitleSize,
                            background = subtitleBackground,
                            bold = subtitleBold,
                            subtitleQuery = subtitleQuery,
                            searchSubtitles = searchSubtitles,
                            manualSearchOpen = manualSearchOpen,
                            onAutoFetch = onAutoFetchSubtitles,
                            onToggleManualSearch = { manualSearchOpen = !manualSearchOpen },
                            onSelect = onSelectSubtitle,
                            onSize = onSubtitleSize,
                            onBackground = onSubtitleBackground,
                            onBold = onSubtitleBold,
                            onSubtitleChosen = onSubtitleChosen,
                        )
                    } else {
                        AudioTracksContent(audioTracks, onSelectAudio)
                    }
                    // Χώρος μετά την τελευταία επιλογή, ώστε να μπορεί να
                    // κυλήσει ολόκληρη πάνω από τη στρογγυλεμένη κάτω άκρη.
                    // Enough scroll tail to lift the final row fully above both the
                    // rounded sheet edge and devices with a tall 3-button navigation bar.
                    Spacer(Modifier.height(56.dp))
                }
            }
        }
    }
}

@Composable
private fun TracksTabButton(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFF3A3A40) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title,
            color = if (selected) Color.White else IptvColors.TextSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun SubtitleTracksContent(
    tracks: List<PlaybackEngine.TrackOption>,
    size: Int,
    background: String,
    bold: Boolean,
    subtitleQuery: String,
    searchSubtitles: (suspend (String) -> List<ExternalSubtitle>)?,
    manualSearchOpen: Boolean,
    onAutoFetch: (() -> Unit)?,
    onToggleManualSearch: () -> Unit,
    onSelect: (String?) -> Unit,
    onSize: (Int) -> Unit,
    onBackground: (String) -> Unit,
    onBold: (Boolean) -> Unit,
    onSubtitleChosen: (ExternalSubtitle) -> Unit,
) {
    PanelSectionLabel(stringResource(R.string.player_available_subtitles))
    TrackPanelRow(
        stringResource(R.string.player_disable),
        stringResource(R.string.player_no_subtitles),
        "×",
        selected = tracks.none { it.selected },
    ) { onSelect(null) }
    if (tracks.isEmpty()) {
        Text(
            stringResource(R.string.player_no_embedded_subtitles),
            color = IptvColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        )
    } else {
        tracks.forEach { track ->
            TrackPanelRow(
                title = track.label,
                subtitle = stringResource(R.string.player_embedded_in_stream),
                badge = languageBadge(track.label),
                selected = track.selected,
                onClick = { onSelect(track.id) },
            )
        }
    }

    if (onAutoFetch != null || searchSubtitles != null) {
        Spacer(Modifier.height(6.dp))
        PanelSectionLabel(stringResource(R.string.player_opensubtitles_search))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            onAutoFetch?.let { action ->
                PanelActionButton(
                    text = stringResource(R.string.player_automatic_search),
                    modifier = Modifier.weight(1f),
                    onClick = action,
                )
            }
            searchSubtitles?.let {
                PanelActionButton(
                    text = stringResource(R.string.player_manual_search),
                    modifier = Modifier.weight(1f),
                    selected = manualSearchOpen,
                    onClick = onToggleManualSearch,
                )
            }
        }
        if (manualSearchOpen && searchSubtitles != null) {
            PlayerSubtitleSearchContent(
                initialQuery = subtitleQuery,
                load = searchSubtitles,
                onSelect = onSubtitleChosen,
            )
        }
    }

    Spacer(Modifier.height(6.dp))
    PanelSectionLabel(stringResource(R.string.player_subtitle_appearance))
    SubtitleSettingRow(stringResource(R.string.player_size), "$size%") { onSize(nextSubtitleSize(size)) }
    SubtitleSettingRow(
        stringResource(R.string.player_bold),
        stringResource(if (bold) R.string.player_yes else R.string.player_no),
    ) { onBold(!bold) }
    SubtitleSettingRow(stringResource(R.string.player_background), localizedSubtitleBackground(background)) {
        onBackground(nextSubtitleBackground(background))
    }
}

@Composable
private fun AudioTracksContent(
    tracks: List<PlaybackEngine.TrackOption>,
    onSelect: (String) -> Unit,
) {
    PanelSectionLabel(stringResource(R.string.player_audio_tracks))
    if (tracks.isEmpty()) {
        Text(
            stringResource(R.string.player_no_separate_audio_tracks),
            color = IptvColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
    } else {
        tracks.forEach { track ->
            TrackPanelRow(
                title = track.label,
                subtitle = stringResource(R.string.player_stream_audio),
                badge = languageBadge(track.label),
                selected = track.selected,
                onClick = { onSelect(track.id) },
            )
        }
    }
}

@Composable
private fun TrackPanelRow(
    title: String,
    subtitle: String,
    badge: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFF29292F) else IptvColors.Surface)
            .border(
                1.dp,
                if (selected) Color.White.copy(alpha = .36f) else Color.Transparent,
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(width = 38.dp, height = 34.dp).clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF34343D)),
            contentAlignment = Alignment.Center,
        ) {
            Text(badge, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                color = IptvColors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun PanelActionButton(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier.height(48.dp).clip(RoundedCornerShape(13.dp))
            .background(if (selected) Color(0xFF303037) else IptvColors.Surface)
            .border(
                1.dp,
                if (selected) Color.White.copy(alpha = .45f) else IptvColors.DividerStrong,
                RoundedCornerShape(13.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun SubtitleSettingRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF121217))
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(value, color = IptvColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PanelSectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = IptvColors.TextSecondary,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(start = 3.dp, top = 4.dp, bottom = 2.dp),
    )
}

private fun languageBadge(label: String): String {
    val clean = label.trim()
    return clean.take(2).uppercase().ifBlank { "•" }
}

private fun nextSubtitleSize(current: Int): Int {
    val index = SUBTITLE_SIZES.indexOf(current)
    return SUBTITLE_SIZES[(index + 1).mod(SUBTITLE_SIZES.size)]
}

private fun nextSubtitleBackground(current: String): String {
    val index = SUBTITLE_BACKGROUNDS.indexOf(current)
    return SUBTITLE_BACKGROUNDS[(index + 1).mod(SUBTITLE_BACKGROUNDS.size)]
}
