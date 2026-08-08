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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors
import androidx.compose.animation.core.tween
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale

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
        modifier.height(58.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (focused) 22.dp else 5.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Color.White else IptvColors.Surface.copy(alpha = .88f))
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = if (focused) Color.Black else IptvColors.TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(11.dp))
        Text(
            query.ifBlank { "Τίτλοι, ηθοποιοί, είδη" },
            color = when {
                focused -> Color.Black
                query.isBlank() -> IptvColors.TextTertiary
                else -> Color.White
            },
            fontSize = 16.sp,
            fontWeight = if (query.isBlank()) FontWeight.SemiBold else FontWeight.ExtraBold,
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
            .shadow(if (focused) 20.dp else 4.dp, CircleShape).clip(CircleShape)
            .background(if (focused) Color.White else IptvColors.Surface.copy(alpha = .88f))
            .onFocusChanged { focused = it.isFocused || it.hasFocus }.clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Mic, "Φωνητική αναζήτηση", tint = if (focused) Color.Black else Color.White)
    }
}

@Composable
internal fun TvSearchKeyboard(
    onCharacter: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    var mode by remember { mutableStateOf("el") }
    val letters = when (mode) {
        "en" -> ('A'..'Z').map(Char::toString)
        "num" -> ('0'..'9').map(Char::toString)
        else -> listOf(
            "Α", "Ά", "Β", "Γ", "Δ", "Ε", "Έ", "Ζ", "Η", "Ή", "Θ", "Ι", "Ί",
            "Κ", "Λ", "Μ", "Ν", "Ξ", "Ο", "Ό", "Π", "Ρ", "Σ", "Τ", "Υ", "Ύ",
            "Φ", "Χ", "Ψ", "Ω", "Ώ"
        )
    }
    val modeKeys = when (mode) {
        "en" -> listOf("ΕΛ", "123")
        "num" -> listOf("ΕΛ", "ABC")
        else -> listOf("ABC", "123")
    }
    val keys = letters + listOf("ΚΕΝΟ", "⌫") + modeKeys + "CLEAR"
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        keys.chunked(6).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                row.forEach { key ->
                    TvKeyboardKey(
                        label = key,
                        onClick = {
                            when (key) {
                                "ΚΕΝΟ" -> onCharacter(" ")
                                "⌫" -> onBackspace()
                                "ABC" -> mode = "en"
                                "ΕΛ" -> mode = "el"
                                "123" -> mode = "num"
                                "CLEAR" -> onClear()
                                else -> onCharacter(key)
                            }
                        },
                        modifier = Modifier.weight(if (key == "ΚΕΝΟ" || key == "CLEAR") 1.35f else 1f)
                    )
                }
                repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TvKeyboardKey(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) motionScale(Motion.TvEmphasisScale) else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "tvSearchKeyScale"
    )
    Box(
        modifier.height(39.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (focused) 15.dp else 0.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) Color.White else IptvColors.SurfaceRaised.copy(alpha = .88f))
            .onFocusChanged { focused = it.isFocused || it.hasFocus }.clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (focused) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun TvSearchSuggestions(
    labels: List<String>,
    onSelect: (String) -> Unit
) {
    if (labels.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = 15.dp)) {
        Text("ΠΡΟΣΦΑΤΑ & ΠΡΟΤΕΙΝΟΜΕΝΑ", color = IptvColors.TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(7.dp))
        labels.take(4).forEach { label -> TvSuggestionRow(label) { onSelect(label) } }
    }
}

@Composable
private fun TvSuggestionRow(label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(8.dp))
            .background(if (focused) Color.White else Color.Transparent)
            .onFocusChanged { focused = it.isFocused || it.hasFocus }.clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("↺", color = if (focused) Color.Black else IptvColors.TextTertiary, fontSize = 12.sp)
        Spacer(Modifier.width(9.dp))
        Text(label, color = if (focused) Color.Black else IptvColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
