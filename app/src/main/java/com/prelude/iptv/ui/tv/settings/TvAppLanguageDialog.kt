package com.prelude.iptv.ui.tv.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.localization.AppLanguage
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.TvDialogTextButton
import com.prelude.iptv.ui.localization.labelRes
import com.prelude.iptv.ui.localization.summaryRes
import com.prelude.iptv.ui.rememberInitialFocus

@Composable
internal fun TvAppLanguageDialog(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    val languages = AppLanguage.entries
    val focusRequesters = remember { List(languages.size) { FocusRequester() } }
    val initialFocus = rememberInitialFocus(key = selected)
    val selectedIndex = languages.indexOf(selected).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IptvColors.BackgroundRaised,
        title = {
            Text(
                stringResource(R.string.language_picker_title),
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.language_picker_subtitle),
                    color = IptvColors.TextTertiary,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(16.dp))
                languages.forEachIndexed { index, language ->
                    TvLanguageOption(
                        language = language,
                        selected = language == selected,
                        focusRequester = focusRequesters[index],
                        initialFocus = initialFocus.takeIf { index == selectedIndex },
                        onPrevious = { focusRequesters[(index - 1 + languages.size) % languages.size].requestFocus() },
                        onNext = { focusRequesters[(index + 1) % languages.size].requestFocus() },
                        onClick = { onSelect(language) },
                    )
                    if (index != languages.lastIndex) Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.language_picker_note),
                    color = IptvColors.TextTertiary,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                )
            }
        },
        confirmButton = {
            TvDialogTextButton(
                label = stringResource(R.string.a11y_close),
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun TvLanguageOption(
    language: AppLanguage,
    selected: Boolean,
    focusRequester: FocusRequester,
    initialFocus: FocusRequester?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val background = when {
        focused -> Color.White
        selected -> Color.White.copy(alpha = .09f)
        else -> Color.Transparent
    }
    val foreground = if (focused) Color.Black else IptvColors.TextPrimary

    Row(
        Modifier.fillMaxWidth()
            .focusRequester(focusRequester)
            .then(if (initialFocus != null) Modifier.focusRequester(initialFocus) else Modifier)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> true.also { onPrevious() }
                    Key.DirectionDown -> true.also { onNext() }
                    else -> false
                }
            }
            .background(background, RoundedCornerShape(12.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .focusable(interactionSource = interaction)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (language == AppLanguage.SYSTEM) Icons.Default.SettingsSuggest else Icons.Default.Language,
            contentDescription = null,
            tint = if (focused) Color.Black else if (selected) IptvColors.Primary else IptvColors.TextSecondary,
        )
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(language.labelRes()),
                color = foreground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(language.summaryRes()),
                color = if (focused) Color.Black.copy(alpha = .64f) else IptvColors.TextTertiary,
                fontSize = 11.sp,
            )
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = if (focused) Color.Black else IptvColors.Primary,
                unselectedColor = if (focused) Color.Black.copy(alpha = .48f) else IptvColors.TextTertiary,
            ),
        )
    }
}
