package com.prelude.iptv.ui.mobile.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.localization.AppLanguage
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.localization.labelRes
import com.prelude.iptv.ui.localization.summaryRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileAppLanguageSheet(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = IptvColors.BackgroundRaised,
        contentColor = IptvColors.TextPrimary,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        ) {
            Text(
                stringResource(R.string.language_picker_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.language_picker_subtitle),
                color = IptvColors.TextTertiary,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(18.dp))
            AppLanguage.entries.forEach { language ->
                MobileLanguageOption(
                    language = language,
                    selected = language == selected,
                    onClick = { onSelect(language) },
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.language_picker_note),
                color = IptvColors.TextTertiary,
                fontSize = 10.sp,
                lineHeight = 15.sp,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun MobileLanguageOption(
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (language == AppLanguage.SYSTEM) Icons.Default.SettingsSuggest else Icons.Default.Language,
            contentDescription = null,
            tint = if (selected) IptvColors.Primary else IptvColors.TextSecondary,
        )
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(language.labelRes()),
                color = IptvColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(language.summaryRes()),
                color = IptvColors.TextTertiary,
                fontSize = 11.sp,
            )
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = IptvColors.Primary,
                unselectedColor = Color.White.copy(alpha = .42f),
            ),
        )
    }
}
