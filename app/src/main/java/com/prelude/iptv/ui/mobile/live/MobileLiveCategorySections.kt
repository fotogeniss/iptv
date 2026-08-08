package com.prelude.iptv.ui.mobile.live

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.R
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.mobile.home.MobileCategoryExplorer
import com.prelude.iptv.ui.mobile.home.MobileCategoryOption

@Composable
internal fun LiveCategorySection(
    title: String,
    channels: List<Channel>,
    favoriteKeys: Set<String>,
    keyOf: (Channel) -> String,
    nowTextFor: (Channel) -> String?,
    onPlay: (Channel) -> Unit,
    onSeeAll: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                color = IptvColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.live_see_all),
                color = IptvColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(channels, key = { channel -> keyOf(channel).ifBlank { channel.name } }) { channel ->
                LiveCard(
                    channel = channel,
                    favorite = keyOf(channel) in favoriteKeys,
                    epgNow = nowTextFor(channel),
                    onClick = { onPlay(channel) },
                )
            }
        }
    }
}

@Composable
internal fun LiveCategoryExplorer(
    options: List<MobileCategoryOption>,
    selectedGroup: String?,
    onSelectGroup: (String?) -> Unit,
) {
    MobileCategoryExplorer(
        options = options,
        selectedId = selectedGroup?.let { "group:$it" } ?: "all",
        onSelect = { id ->
            onSelectGroup(id.removePrefix("group:").takeIf { id != "all" })
        },
        hint = stringResource(R.string.live_category_explore_hint),
        sheetTitle = stringResource(R.string.live_categories_title),
    )
}
