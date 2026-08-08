package com.prelude.iptv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class PresentationMetrics(
    val horizontalPadding: Dp,
    val sectionSpacing: Dp,
    val itemSpacing: Dp,
    val compact: Boolean,
    val television: Boolean
)

@Composable
fun rememberPresentationMetrics(): PresentationMetrics {
    val configuration = LocalConfiguration.current
    val tv = isTvDevice()
    val compact = !tv && configuration.screenWidthDp < 380
    return PresentationMetrics(
        horizontalPadding = when {
            tv -> 36.dp
            compact -> 14.dp
            else -> 16.dp
        },
        sectionSpacing = if (tv) 28.dp else 22.dp,
        itemSpacing = if (tv) 14.dp else 10.dp,
        compact = compact,
        television = tv
    )
}
