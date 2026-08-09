package com.prelude.iptv.ui.mobile.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.localization.labelRes
import com.prelude.iptv.ui.localization.resources

@Composable
internal fun MobileLegalHero() {
    Column(
        Modifier.fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(IptvColors.Background, Color(0xFF18090A), IptvColors.Background)
                )
            )
            .padding(horizontal = 20.dp, vertical = 25.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(Color(0xFFFF5961), RoundedCornerShape(50)))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.legal_privacy_eyebrow),
                color = Color(0xFFFF5961),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.legal_privacy_hero_title),
            color = IptvColors.TextPrimary,
            fontSize = 28.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(9.dp))
        Text(
            stringResource(R.string.legal_privacy_hero_body),
            color = IptvColors.TextSecondary,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MobileLegalFact(
                Icons.Default.Check,
                stringResource(R.string.legal_fact_no_analytics_title),
                stringResource(R.string.legal_fact_no_analytics_body),
                Modifier.weight(1f),
            )
            MobileLegalFact(
                Icons.Default.Check,
                stringResource(R.string.legal_fact_no_ads_title),
                stringResource(R.string.legal_fact_no_ads_body),
                Modifier.weight(1f),
            )
            MobileLegalFact(
                Icons.Default.CloudOff,
                stringResource(R.string.legal_fact_no_sync_title),
                stringResource(R.string.legal_fact_no_sync_body),
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MobileLegalFact(icon: ImageVector, title: String, subtitle: String, modifier: Modifier) {
    Column(
        modifier.height(94.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(IptvColors.Surface)
            .border(1.dp, IptvColors.Divider, RoundedCornerShape(15.dp))
            .padding(9.dp)
    ) {
        Box(
            Modifier.size(27.dp).background(IptvColors.Success.copy(alpha = 0.13f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = IptvColors.Success, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(title, color = IptvColors.TextPrimary, fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = IptvColors.TextTertiary, fontSize = 7.sp, lineHeight = 9.sp, maxLines = 2)
    }
}

@Composable
internal fun MobileLegalTabs(selected: MobileLegalTab, onSelected: (MobileLegalTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(IptvColors.Background)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF111111))
            .border(1.dp, IptvColors.Divider, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MobileLegalTab.entries.forEach { tab ->
            val active = tab == selected
            Box(
                Modifier.weight(1f).height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) Color(0xFFF2F2F2) else Color.Transparent)
                    .clickable { onSelected(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(tab.labelRes()),
                    color = if (active) Color(0xFF111111) else IptvColors.TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
internal fun MobileLegalSectionTitle(title: String) {
    Text(
        title,
        color = Color(0xFFD4D4D4),
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(start = 4.dp, top = 19.dp, bottom = 9.dp),
    )
}

@Composable
internal fun MobileLegalDisclosureCard(
    disclosures: List<MobileLegalDisclosure>,
    expanded: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(IptvColors.Surface)
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(17.dp))
    ) {
        disclosures.forEachIndexed { index, disclosure ->
            val isExpanded = disclosure.id in expanded
            val copy = disclosure.resources()
            Row(
                Modifier.fillMaxWidth().clickable { onToggle(disclosure.id) }.padding(13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(42.dp).background(IptvColors.SurfaceRaised, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(disclosure.icon.imageVector(), null, tint = IptvColors.TextPrimary, modifier = Modifier.size(21.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(copy.title), color = IptvColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(copy.summary), color = IptvColors.TextTertiary, fontSize = 9.sp, lineHeight = 13.sp)
                }
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = IptvColors.TextTertiary,
                    modifier = Modifier.size(20.dp).rotate(if (isExpanded) 90f else 0f),
                )
            }
            AnimatedVisibility(isExpanded) {
                Text(
                    stringResource(copy.details),
                    color = IptvColors.TextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.fillMaxWidth().padding(start = 67.dp, end = 15.dp, bottom = 15.dp),
                )
            }
            if (index != disclosures.lastIndex) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.07f)))
            }
        }
    }
}

@Composable
internal fun MobileLegalNotice(title: String, body: String, warning: Boolean = false) {
    val accent = if (warning) IptvColors.Warning else Color(0xFFFF626A)
    Column(
        Modifier.fillMaxWidth().padding(top = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.075f))
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(15.dp)
    ) {
        Text(title, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        Text(body, color = IptvColors.TextSecondary, fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
internal fun MobileLegalTermsCard(terms: List<MobileLegalTerm>) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(IptvColors.Surface)
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(17.dp)).padding(17.dp)
    ) {
        terms.forEachIndexed { index, term ->
            val copy = term.resources()
            if (index > 0) Spacer(Modifier.height(18.dp))
            Text(stringResource(copy.title), color = IptvColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(copy.body), color = IptvColors.TextSecondary, fontSize = 10.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
internal fun MobileLegalServicesCard(services: List<MobileLegalService>) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(IptvColors.Surface)
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(17.dp))
    ) {
        services.forEachIndexed { index, service ->
            val copy = service.resources()
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(44.dp).background(IptvColors.SurfaceRaised, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(service.badge, color = IptvColors.TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(copy.title), color = IptvColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(copy.description), color = IptvColors.TextTertiary, fontSize = 9.sp, lineHeight = 13.sp)
                    Spacer(Modifier.height(7.dp))
                    Text(stringResource(copy.status), color = Color(0xFFFF5961), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
            if (index != services.lastIndex) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.07f)))
            }
        }
    }
}

private fun MobileLegalIcon.imageVector(): ImageVector = when (this) {
    MobileLegalIcon.STORAGE -> Icons.Default.Storage
    MobileLegalIcon.FAVORITES -> Icons.Default.FavoriteBorder
    MobileLegalIcon.CACHE -> Icons.Default.Cached
    MobileLegalIcon.NETWORK -> Icons.Default.Language
}
