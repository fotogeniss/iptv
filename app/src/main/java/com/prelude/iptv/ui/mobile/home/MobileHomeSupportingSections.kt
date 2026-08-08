package com.prelude.iptv.ui.mobile.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.R

@Composable
internal fun CatalogCategoryExplorer(
    selectedDestination: String,
    options: List<MobileCategoryOption>,
    selectedGroup: String?,
    onSelectGroup: (String?) -> Unit,
) {
    val movies = selectedDestination == "movies"
    MobileCategoryExplorer(
        options = options,
        selectedId = selectedGroup?.let { "group:$it" } ?: "all",
        onSelect = { id ->
            onSelectGroup(id.removePrefix("group:").takeIf { id != "all" })
        },
        hint = if (movies) {
            stringResource(R.string.home_movie_explore_hint)
        } else {
            stringResource(R.string.home_series_explore_hint)
        },
        sheetTitle = stringResource(
            if (movies) R.string.home_movie_categories else R.string.home_series_categories
        ),
    )
}

@Composable
internal fun SuggestionsEmptyState() {
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        Text(
            stringResource(R.string.home_suggestions_heading),
            color = IptvColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(IptvColors.Surface)
                .padding(horizontal = 18.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.home_suggestions_empty_title),
                color = IptvColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.home_suggestions_empty_body),
                color = IptvColors.TextTertiary,
                fontSize = 12.5.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
