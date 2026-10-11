package com.prelude.iptv.ui.tv.search

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.prelude.iptv.ui.icons.filled.Mic
import com.prelude.iptv.ui.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.design.horizonBelow
import androidx.compose.foundation.border
import com.prelude.iptv.R
import com.prelude.iptv.ui.IptvColors
import androidx.compose.animation.core.tween
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale
import com.prelude.iptv.ui.SearchKeyboardAction
import com.prelude.iptv.ui.SearchKeyboardMode
import com.prelude.iptv.ui.SearchKeyboardPolicy
import com.prelude.iptv.ui.localization.labelRes

@Composable
internal fun TvSearchEntryRow(
    query: String,
    focusRequester: FocusRequester,
    onEdit: () -> Unit,
    onVoice: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TvSearchEntrySurface(
            query = query,
            onClick = onEdit,
            modifier = Modifier.weight(1f).focusRequester(focusRequester)
        )
        TvRoundAction(onClick = onVoice)
    }
}

@Composable
private fun TvSearchEntrySurface(query: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) motionScale(1.018f) else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "tvSearchEntryScale"
    )
    Row(
        modifier.height(58.dp)
            .horizonBelow(if (focused) 1f else 0f, widthFraction = 0.8f, gap = 4.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) IptvColors.SurfaceSelected else IptvColors.Surface)
            .border(1.dp, if (focused) IptvColors.DividerStrong else IptvColors.Divider, RoundedCornerShape(16.dp))
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = IptvColors.TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(11.dp))
        Text(
            query.ifBlank { stringResource(R.string.search_field_hint) },
            color = when {
                query.isBlank() -> IptvColors.TextTertiary
                else -> IptvColors.TextPrimary
            },
            fontSize = 16.sp,
            fontWeight = if (query.isBlank()) FontWeight.Normal else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TvRoundAction(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) motionScale(Motion.TvEmphasisScale) else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "tvSearchVoiceScale"
    )
    Box(
        Modifier.size(58.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .horizonBelow(if (focused) 1f else 0f, gap = 4.dp)
            .clip(CircleShape)
            .background(if (focused) IptvColors.SurfaceSelected else IptvColors.Surface)
            .border(1.dp, if (focused) IptvColors.DividerStrong else IptvColors.Divider, CircleShape)
            .onFocusChanged { focused = it.isFocused || it.hasFocus }.clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Mic, stringResource(R.string.search_voice), tint = IptvColors.TextPrimary)
    }
}

@Composable
internal fun TvSearchKeyboard(
    onCharacter: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    val language = LocalConfiguration.current.locales[0].language
    var mode by remember(language) { mutableStateOf(SearchKeyboardPolicy.initialMode(language)) }
    val keys = SearchKeyboardPolicy.keys(mode)
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        keys.chunked(6).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                row.forEach { key ->
                    val label = when (key.action) {
                        SearchKeyboardAction.CHARACTER -> key.character
                        SearchKeyboardAction.BACKSPACE -> "⌫"
                        else -> stringResource(key.action.labelRes())
                    }
                    TvKeyboardKey(
                        label = label,
                        a11yDescription = if (key.action == SearchKeyboardAction.BACKSPACE) {
                            stringResource(R.string.search_keyboard_backspace)
                        } else null,
                        onClick = {
                            when (key.action) {
                                SearchKeyboardAction.CHARACTER -> onCharacter(key.character)
                                SearchKeyboardAction.SPACE -> onCharacter(" ")
                                SearchKeyboardAction.BACKSPACE -> onBackspace()
                                SearchKeyboardAction.CLEAR -> onClear()
                                SearchKeyboardAction.GREEK -> mode = SearchKeyboardMode.GREEK
                                SearchKeyboardAction.LATIN -> mode = SearchKeyboardMode.LATIN
                                SearchKeyboardAction.NUMERIC -> mode = SearchKeyboardMode.NUMERIC
                            }
                        },
                        modifier = Modifier.weight(
                            if (key.action == SearchKeyboardAction.SPACE ||
                                key.action == SearchKeyboardAction.CLEAR
                            ) 1.35f else 1f
                        )
                    )
                }
                repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TvKeyboardKey(
    label: String,
    a11yDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) motionScale(Motion.TvEmphasisScale) else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "tvSearchKeyScale"
    )
    Box(
        modifier.height(42.dp)
            .horizonBelow(if (focused) 1f else 0f, widthFraction = 0.7f, gap = 3.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (a11yDescription != null) Modifier.semantics {
                    contentDescription = a11yDescription
                } else Modifier
            )
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) IptvColors.SurfaceSelected else IptvColors.Surface)
            .border(1.dp, if (focused) IptvColors.DividerStrong else IptvColors.Divider, RoundedCornerShape(10.dp))
            .onFocusChanged { focused = it.isFocused || it.hasFocus }.clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = IptvColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun TvSearchSuggestions(
    labels: List<String>,
    onSelect: (String) -> Unit
) {
    if (labels.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = 15.dp)) {
        Text(stringResource(R.string.search_recent_recommended), color = IptvColors.TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(7.dp))
        labels.take(4).forEach { label -> TvSuggestionRow(label) { onSelect(label) } }
    }
}

@Composable
private fun TvSuggestionRow(label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(38.dp)
            .horizonBelow(if (focused) 1f else 0f, gap = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) IptvColors.SurfaceSelected else Color.Transparent)
            .onFocusChanged { focused = it.isFocused || it.hasFocus }.clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("↺", color = IptvColors.TextTertiary, fontSize = 13.sp)
        Spacer(Modifier.width(9.dp))
        Text(label, color = if (focused) IptvColors.TextPrimary else IptvColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
