@file:android.annotation.SuppressLint("ProduceStateDoesNotAssignValue")

package com.prelude.iptv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.R
import com.prelude.iptv.player.TrackLabelPolicy
import com.prelude.iptv.ui.requestFocusWithRetry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** Η χειροκίνητη αναζήτηση που αναπτύσσεται μέσα στο κοινό CC/audio panel. */
@Composable
internal fun PlayerSubtitleSearchContent(
    initialQuery: String,
    load: suspend (String) -> List<ExternalSubtitle>,
    onSelect: (ExternalSubtitle) -> Unit,
) {
    val firstResultFocus = remember { FocusRequester() }
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }
    var searchTick by remember { mutableStateOf(0) }
    var queryFocused by remember { mutableStateOf(false) }
    val normalizedQuery = SubtitleAutoSearchPolicy.normalizedQuery(query)
    val currentLoad by rememberUpdatedState(load)
    val results by produceState<List<ExternalSubtitle>?>(
        initialValue = null,
        normalizedQuery,
        searchTick,
    ) {
        value = null
        if (!SubtitleAutoSearchPolicy.shouldSearch(normalizedQuery)) {
            value = emptyList()
            return@produceState
        }
        // Query changes cancel this producer automatically. The debounce avoids
        // sending one OpenSubtitles request per keystroke while still searching
        // immediately after the user pauses typing.
        delay(SubtitleAutoSearchPolicy.DEBOUNCE_MS)
        value = try {
            currentLoad(normalizedQuery)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyList()
        }
    }

    LaunchedEffect(results, queryFocused) {
        if (!queryFocused && !results.isNullOrEmpty()) {
            firstResultFocus.requestFocusWithRetry()
        }
    }

    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.player_subtitle_search_title), color = IptvColors.TextSecondary) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 55.dp)
                .onFocusChanged { queryFocused = it.isFocused },
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable { searchTick++ },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Search, stringResource(R.string.player_search), tint = Color.Black)
        }
    }

    val currentResults = results
    when {
        currentResults == null -> Text(
            stringResource(R.string.player_searching),
            color = IptvColors.TextSecondary,
            fontSize = 12.sp,
        )
        currentResults.isEmpty() -> Text(
            stringResource(R.string.player_no_subtitle_results),
            color = IptvColors.TextSecondary,
            fontSize = 12.sp,
        )
        else -> {
            Text(
                pluralStringResource(
                    R.plurals.player_subtitle_result_count,
                    currentResults.size,
                    currentResults.size,
                ),
                color = IptvColors.TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                currentResults.take(MAX_VISIBLE_RESULTS).forEachIndexed { index, item ->
                    SubtitleSearchResultRow(
                        item = item,
                        focusRequester = if (index == 0) firstResultFocus else null,
                        onClick = { onSelect(item) },
                    )
                }
            }
        }
    }
}

internal object SubtitleAutoSearchPolicy {
    const val DEBOUNCE_MS = 400L
    private val repeatedWhitespace = Regex("\\s+")

    fun normalizedQuery(query: String): String =
        query.trim().replace(repeatedWhitespace, " ")

    fun shouldSearch(query: String): Boolean = normalizedQuery(query).isNotBlank()
}

@Composable
private fun SubtitleSearchResultRow(
    item: ExternalSubtitle,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val languageName = TrackLabelPolicy.languageName(item.language, locale)
        .ifBlank { stringResource(R.string.player_unknown_language) }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (focused) Color(0xFF34343B) else Color(0xFF242429))
            .border(1.dp, if (focused) Color.White.copy(alpha = .72f) else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF373740)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                item.language.uppercase().take(2),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Column(Modifier.fillMaxWidth().padding(start = 49.dp)) {
            Text(
                item.label,
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(3.dp))
            Text(
                stringResource(R.string.player_subtitle_provider_label, languageName),
                color = IptvColors.TextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.player_match_percent, item.matchPercent),
                color = Color(0xFF4BD486),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

private const val MAX_VISIBLE_RESULTS = 20
