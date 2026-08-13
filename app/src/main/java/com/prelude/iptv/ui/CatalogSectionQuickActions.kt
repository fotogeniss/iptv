package com.prelude.iptv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R

/** The approved post-download actions shared by Live, Movies and Series. */
@Composable
fun CatalogSectionQuickActions(
    contentType: String,
    itemCount: Int,
    categoryCount: Int,
    onCategories: () -> Unit,
    onSort: () -> Unit,
    onFavorites: () -> Unit,
    onRefresh: () -> Unit,
    categoryFocus: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(Color.Black)) {
        Row(
            Modifier.fillMaxWidth()
                .focusGroup()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                Triple(R.string.catalog_quick_categories, true, onCategories),
                Triple(R.string.catalog_sort, false, onSort),
                Triple(R.string.catalog_favorites, false, onFavorites),
                Triple(R.string.catalog_refresh, false, onRefresh),
            ).forEachIndexed { index, (labelRes, primary, action) ->
                Text(
                    stringResource(labelRes),
                    color = if (primary) Color.Black else Color(0xFFD0D2D7),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .then(if (index == 0 && categoryFocus != null) Modifier.focusRequester(categoryFocus) else Modifier)
                        .background(if (primary) Color(0xFFE9EAEC) else Color.Transparent, RoundedCornerShape(99.dp))
                        .border(1.dp, if (primary) Color(0xFFE9EAEC) else Color(0xFF34363C), RoundedCornerShape(99.dp))
                        .tvFocus(RoundedCornerShape(99.dp), tint = false)
                        .clickable(onClick = action)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
        Text(
            stringResource(
                when (contentType) {
                    "series" -> R.string.catalog_downloaded_summary_series
                    "vod" -> R.string.catalog_downloaded_summary_movies
                    else -> R.string.catalog_downloaded_summary_live
                },
                itemCount,
                categoryCount,
            ),
            color = Color(0xFF7D818A),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 9.dp),
        )
    }
}
