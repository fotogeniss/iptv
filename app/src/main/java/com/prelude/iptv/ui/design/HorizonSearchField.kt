package com.prelude.iptv.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.AppFonts
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.icons.filled.Close
import com.prelude.iptv.ui.icons.filled.Search
import java.text.Normalizer

/**
 * Πεδίο αναζήτησης του Horizon (ρυθμίσεις, αναζήτηση). Με focus η επιφάνεια
 * ανεβαίνει και εμφανίζεται από κάτω η γραμμή-ορίζοντας.
 */
@Composable
fun HorizonSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    clearLabel: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val lineAlpha by animateFloatAsState(if (focused) 1f else 0f, label = "searchHorizon")
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(50.dp)
            .horizonBelow(lineAlpha, widthFraction = 0.8f, gap = 0.dp)
            .clip(shape)
            .background(if (focused) IptvColors.SurfaceRaised else IptvColors.Surface)
            .border(1.dp, if (focused) Color.Transparent else IptvColors.DividerStrong, shape)
            .padding(start = 14.dp, end = if (trailing != null) 4.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Default.Search, null, tint = IptvColors.TextSecondary, modifier = Modifier.size(21.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(placeholder, color = IptvColors.TextTertiary, fontSize = 14.5.sp, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                interactionSource = interaction,
                textStyle = TextStyle(color = IptvColors.TextPrimary, fontSize = 15.sp, fontFamily = AppFonts.Body),
                cursorBrush = SolidColor(IptvColors.Glow),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Default.Close,
                clearLabel,
                tint = IptvColors.TextSecondary,
                modifier = Modifier.size(20.dp).clip(CircleShape).clickable { onValueChange("") },
            )
        }
        trailing?.invoke()
    }
}

/**
 * Τονίζει το κομμάτι του [text] που ταιριάζει με το [query], αγνοώντας πεζά/κεφαλαία
 * και τόνους («σπορ» βρίσκει το «Σπόρ»). Αν δεν ταιριάζει, επιστρέφει το κείμενο ως έχει.
 */
fun highlightMatch(text: String, query: String, color: Color = IptvColors.Glow): AnnotatedString {
    val needle = foldForSearch(query.trim())
    if (needle.isEmpty()) return AnnotatedString(text)
    // Κανονικοποιούμε χαρακτήρα-χαρακτήρα κρατώντας από ποια θέση του αρχικού ήρθε
    // κάθε χαρακτήρας, ώστε το εύρος να αντιστοιχιστεί πίσω σωστά.
    val folded = StringBuilder()
    val origin = ArrayList<Int>()
    text.forEachIndexed { index, char ->
        foldForSearch(char.toString()).forEach { folded.append(it); origin.add(index) }
    }
    val at = folded.indexOf(needle)
    if (at < 0) return AnnotatedString(text)
    val start = origin[at]
    val end = origin[at + needle.length - 1] + 1
    return buildAnnotatedString {
        append(text.substring(0, start))
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) { append(text.substring(start, end)) }
        append(text.substring(end))
    }
}

/** Πεζά χωρίς τόνους/διαλυτικά, για συγκρίσεις αναζήτησης. */
fun foldForSearch(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
