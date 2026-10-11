package com.prelude.iptv.ui.mobile.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.ui.AppFonts
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.design.foldForSearch
import com.prelude.iptv.ui.icons.filled.ChevronRight
import java.text.Normalizer

/*
 * Δομικά στοιχεία των Ρυθμίσεων στο Horizon design
 * (prototypes/redesign_horizon_screens.html, ενότητα 1).
 * Κάρτες-ομάδες με λεπτές διαχωριστικές, τιμή δεξιά σε κάθε γραμμή,
 * segmented επιλογές αντί για διαλόγους όπου οι επιλογές είναι λίγες.
 */

private val CardShape = RoundedCornerShape(18.dp)

/** Μεγάλος τίτλος οθόνης με τη display γραμματοσειρά. */
@Composable
internal fun HorizonSettingsTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        color = IptvColors.TextPrimary,
        fontFamily = AppFonts.Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 36.sp,
        modifier = modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 12.dp),
    )
}

/** Κεφαλαία χωρίς τόνους, όπως γράφονται σωστά τα ελληνικά κεφαλαία. */
internal fun upperWithoutAccents(text: String): String =
    Normalizer.normalize(text.uppercase(), Normalizer.Form.NFD).replace("\u0301", "")

/** Κανονικοποίηση για αναζήτηση: πεζά, χωρίς τόνους/διαλυτικά. */
internal fun searchKey(text: String): String = foldForSearch(text)

/** Ετικέτα ομάδας πάνω από κάθε κάρτα. */
@Composable
internal fun HorizonGroupLabel(title: String, modifier: Modifier = Modifier) {
    Text(
        upperWithoutAccents(title),
        color = IptvColors.TextTertiary,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 8.dp),
    )
}

/**
 * Κάρτα που κρατά γραμμές ρυθμίσεων. Το φόντο της κάρτας είναι το χρώμα της
 * διαχωριστικής και οι γραμμές απέχουν 1dp, οπότε οι διαχωριστικές βγαίνουν μόνες τους.
 */
@Composable
internal fun HorizonSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth()
            .clip(CardShape)
            .background(IptvColors.Divider)
            .border(1.dp, IptvColors.Divider, CardShape),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        content = content,
    )
}

/** Μία γραμμή ρύθμισης: εικονίδιο, τίτλος, προαιρετική τιμή δεξιά και βελάκι. */
@Composable
internal fun HorizonSettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(IptvColors.Surface)
            .heightIn(min = 58.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        if (icon != null) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(IptvColors.SurfaceSelected),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = IptvColors.TextPrimary, modifier = Modifier.size(19.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = IptvColors.TextPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    color = IptvColors.TextTertiary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!value.isNullOrBlank()) {
            Text(
                value,
                color = IptvColors.TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.6f, fill = false),
            )
        }
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(
                Icons.Default.ChevronRight,
                null,
                tint = IptvColors.TextTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Γραμμή με περιεχόμενο κάτω από τον τίτλο (segmented, προεπισκόπηση κ.λπ.). */
@Composable
internal fun HorizonSettingsBlock(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().background(IptvColors.Surface).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                color = IptvColors.TextPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (!value.isNullOrBlank()) {
                Text(value, color = IptvColors.TextSecondary, fontSize = 13.sp)
            }
        }
        content()
    }
}

@Composable
internal fun HorizonSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = IptvColors.Primary,
            checkedBorderColor = IptvColors.Primary,
            uncheckedThumbColor = IptvColors.TextSecondary,
            uncheckedTrackColor = IptvColors.SurfaceSelected,
            uncheckedBorderColor = IptvColors.SurfaceSelected,
        ),
    )
}

/** Segmented επιλογή για 2–4 τιμές: η επιλεγμένη γίνεται λευκή. */
@Composable
internal fun <T> HorizonSegmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IptvColors.SurfaceSelected)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { (key, label) ->
            val on = key == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) IptvColors.TextPrimary else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(key) }
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (on) IptvColors.Background else IptvColors.TextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Κάρτα προφίλ στην κορυφή: ποιος είσαι και ποια πηγή είναι ενεργή. */
@Composable
internal fun HorizonProfileCard(
    profileName: String,
    sourceName: String?,
    onProfile: () -> Unit,
    onPremium: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF2A1520), IptvColors.Surface)))
            .border(1.dp, IptvColors.DividerStrong, shape)
            .clickable(role = Role.Button, onClick = onProfile)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF7A2C40), Color(0xFF2A1D3D)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                profileName.trim().take(1).uppercase().ifBlank { "P" },
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                profileName,
                color = IptvColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(6.dp).background(
                        if (sourceName != null) IptvColors.Success else IptvColors.TextTertiary,
                        CircleShape,
                    )
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    if (sourceName != null) stringResource(R.string.settings_profile_source, sourceName)
                    else stringResource(R.string.settings_no_source),
                    color = IptvColors.TextSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            stringResource(R.string.settings_premium_badge),
            color = IptvColors.Glow,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, IptvColors.DividerStrong, RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onPremium)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
