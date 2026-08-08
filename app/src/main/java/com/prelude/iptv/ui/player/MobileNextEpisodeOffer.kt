package com.prelude.iptv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.prelude.iptv.ui.IptvColors

/** Self-contained countdown card shown shortly before a series episode ends. */
@Composable
internal fun MobileNextEpisodeOffer(
    title: String,
    imageUrl: String?,
    autoPlayInSeconds: Int,
    onPlay: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 82.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Color.Black.copy(alpha = .90f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(106.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(IptvColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Default.PlayArrow, null, tint = IptvColors.TextSecondary)
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            val minutes = autoPlayInSeconds / 60
            val seconds = autoPlayInSeconds % 60
            Text(
                "ΕΠΟΜΕΝΟ ΕΠΕΙΣΟΔΙΟ · ΣΕ $minutes:${seconds.toString().padStart(2, '0')}",
                color = Color(0xFFFF777D),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp, bottom = 6.dp),
            )
            Text(
                "▶ Παίξε τώρα",
                color = Color.Black,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.White)
                    .clickable(onClick = onPlay)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.Top).size(34.dp),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Κλείσιμο επόμενου επεισοδίου",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
