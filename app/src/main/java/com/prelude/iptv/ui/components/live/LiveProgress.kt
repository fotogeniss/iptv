package com.prelude.iptv.ui.components.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.prelude.iptv.ui.IptvColors

@Composable
fun LiveProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Int = 4
) {
    Box(
        modifier
            .height(height.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Color.White.copy(alpha = .18f))
    ) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(IptvColors.Primary, RoundedCornerShape(99.dp))
        )
    }
}
