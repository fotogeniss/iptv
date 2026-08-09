package com.prelude.iptv.ui.mobile.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prelude.iptv.R
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.SubtitleSearchPolicy
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.localization.localizedSeasonCount

@Composable
internal fun MobileSeasonHeader(
    seasons: List<Pair<String, List<Channel>>>,
    selected: Int,
    descending: Boolean,
    onSelected: (Int) -> Unit,
    onToggleOrder: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selected, seasons.size) {
        if (seasons.isNotEmpty()) {
            listState.animateScrollToItem(selected.coerceIn(seasons.indices))
        }
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 18.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = stringResource(R.string.details_episodes),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = localizedSeasonCount(seasons.size),
                color = IptvColors.TextTertiary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            itemsIndexed(seasons, key = { index, season -> "season:$index:${season.first}" }) { index, season ->
                val active = index == selected
                Column(
                    modifier = Modifier
                        .clickable { onSelected(index) }
                        .padding(top = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(
                            R.string.details_season_number,
                            SubtitleSearchPolicy.seasonNumber(season.first, index + 1) ?: index + 1,
                        ),
                        color = if (active) Color.White else IptvColors.TextTertiary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                    Box(
                        Modifier
                            .padding(top = 9.dp)
                            .height(3.dp)
                            .fillMaxWidth()
                            .background(
                                color = if (active) IptvColors.Primary else Color.Transparent,
                                shape = RoundedCornerShape(99.dp)
                            )
                    )
                }
            }
        }

        Text(
            text = stringResource(
                if (descending) R.string.details_order_last else R.string.details_order_first
            ),
            color = IptvColors.TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(start = 16.dp, top = 14.dp)
                .clickable(onClick = onToggleOrder)
                .padding(vertical = 6.dp, horizontal = 2.dp)
        )
    }
}
