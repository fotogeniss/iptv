package com.prelude.iptv.ui.tv.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.library.LibraryHubTab
import com.prelude.iptv.ui.components.library.LibrarySort
import com.prelude.iptv.ui.components.library.PremiumLibraryContent
import com.prelude.iptv.ui.localization.labelRes
import com.prelude.iptv.ui.tvFocus

@Suppress("DEPRECATION")
private val acceptedSortIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Sort

@Composable
internal fun TvLibraryHeading(
    content: PremiumLibraryContent,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).tvFocus(RoundedCornerShape(8.dp), tint = false)
                    .clickable(onClick = onBack).focusable().padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.library_back), tint = Color.White)
                Spacer(Modifier.width(7.dp))
                Text(stringResource(R.string.library_title), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(17.dp))
            Text("PRELUDE LIBRARY", color = IptvColors.Success, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.library_hero_headline), color = Color.White, fontSize = 38.sp, lineHeight = 39.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(7.dp))
            Text(stringResource(R.string.library_hero_subheadline), color = IptvColors.TextSecondary, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            TvLibraryStat(content.myList.size, stringResource(R.string.library_stat_my_list))
            TvLibraryStat(content.continueWatching.size, stringResource(R.string.library_stat_continue))
            TvLibraryStat(content.history.size, stringResource(R.string.library_stat_history))
        }
    }
}

@Composable
private fun TvLibraryStat(value: Int, label: String) {
    Column(Modifier.width(112.dp).background(Color(0xB3121519), RoundedCornerShape(13.dp)).padding(13.dp)) {
        Text(value.toString(), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
        Text(label, color = IptvColors.TextSecondary, fontSize = 8.sp, letterSpacing = .7.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun TvLibraryControls(
    selected: LibraryHubTab,
    onSelect: (LibraryHubTab) -> Unit,
    sort: LibrarySort,
    onSort: () -> Unit,
    managementMode: Boolean,
    onManage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            LibraryHubTab.entries.forEach { tab ->
                TvLibraryPill(stringResource(tab.labelRes()), tab == selected) { onSelect(tab) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvLibraryTool(acceptedSortIcon, stringResource(sort.labelRes()), onSort)
            TvLibraryTool(
                Icons.Default.Check,
                stringResource(if (managementMode) R.string.library_action_done else R.string.library_action_manage),
                onManage
            )
        }
    }
}

@Composable
private fun TvLibraryPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.White else IptvColors.TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.height(38.dp).clip(RoundedCornerShape(99.dp))
            .background(if (selected) IptvColors.Primary else Color(0xB3121519))
            .tvFocus(RoundedCornerShape(99.dp), tint = false).clickable(onClick = onClick).focusable()
            .padding(horizontal = 15.dp, vertical = 11.dp)
    )
}

@Composable
private fun TvLibraryTool(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.height(38.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xB3121519))
            .tvFocus(RoundedCornerShape(9.dp), tint = false).clickable(onClick = onClick).focusable()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.height(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
