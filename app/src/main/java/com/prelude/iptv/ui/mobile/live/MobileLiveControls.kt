package com.prelude.iptv.ui.mobile.live

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.R
import com.prelude.iptv.ui.mobile.navigation.MobileSettingsAction

internal enum class LiveChannelLayout { LIST, GRID }

@Composable
internal fun LiveHeader(
    title: String,
    onBack: () -> Unit,
    onOpenEpg: (() -> Unit)?,
    onOpenCategories: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.live_back),
            tint = IptvColors.TextPrimary,
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(99.dp))
                .clickable(onClick = onBack)
        )
        Text(
            title,
            color = IptvColors.TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        Icon(
            Icons.Default.Category,
            stringResource(R.string.catalog_categories_groups),
            tint = IptvColors.TextPrimary,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(IptvColors.Surface)
                .clickable(onClick = onOpenCategories)
                .padding(8.dp),
        )
        Spacer(Modifier.width(6.dp))
        if (onOpenEpg != null) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(IptvColors.Surface, RoundedCornerShape(10.dp))
                    .border(1.5.dp, IptvColors.DividerStrong, RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenEpg)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Tv, null, tint = IptvColors.TextPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.live_epg), color = IptvColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.width(6.dp))
        MobileSettingsAction(onClick = onSettings, modifier = Modifier.size(38.dp))
    }
}

@Composable
internal fun LiveSearchField(
    value: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    categoryLayout: LiveChannelLayout?,
    onToggleLayout: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(IptvColors.Surface)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Search, null,
                tint = IptvColors.TextTertiary,
                modifier = Modifier.size(26.dp)
            )
            BasicTextFieldRow(value = value, onChange = onChange)
            if (value.isNotBlank()) {
                Icon(
                    Icons.Default.Close, stringResource(R.string.live_clear_search),
                    tint = IptvColors.TextSecondary,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .clickable(onClick = onClear)
                )
            }
        }
        if (categoryLayout != null) {
            val switchToGrid = categoryLayout == LiveChannelLayout.LIST
            Icon(
                imageVector = if (switchToGrid) Icons.Default.GridView else Icons.Default.Menu,
                contentDescription = stringResource(
                    if (switchToGrid) R.string.live_show_grid else R.string.live_show_list
                ),
                tint = IptvColors.TextPrimary,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .clickable(onClick = onToggleLayout)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun RowScope.BasicTextFieldRow(value: String, onChange: (String) -> Unit) {
    Box(Modifier.weight(1f).padding(start = 12.dp, top = 16.dp, bottom = 16.dp)) {
        if (value.isEmpty()) {
            Text(
                stringResource(R.string.live_search_hint),
                color = IptvColors.TextTertiary,
                fontSize = 16.sp
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = IptvColors.TextPrimary, fontSize = 16.sp),
            cursorBrush = SolidColor(IptvColors.Primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
