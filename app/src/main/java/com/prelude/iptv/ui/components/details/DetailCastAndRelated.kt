package com.prelude.iptv.ui.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.ContentQualityPolicy
import com.prelude.iptv.ui.CastMember
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.StreamingRadius

@Composable
fun PremiumCastCard(
    member: CastMember,
    width: Dp,
    modifier: Modifier = Modifier
) {
    Column(modifier.width(width)) {
        Box(
            Modifier
                .width(width)
                .aspectRatio(0.8f)
                .clip(RoundedCornerShape(StreamingRadius.Card))
                .background(IptvColors.Surface)
                .border(1.dp, IptvColors.Divider, RoundedCornerShape(StreamingRadius.Card)),
            contentAlignment = Alignment.Center
        ) {
            if (!member.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = member.photoUrl,
                    contentDescription = member.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    detailInitials(member.name),
                    color = IptvColors.TextSecondary,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            member.name,
            color = IptvColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (member.role.isNotBlank()) {
            Text(
                member.role,
                color = IptvColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PremiumRelatedCard(
    channel: Channel,
    width: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quality = ContentQualityPolicy.label(channel.name, channel.group)
    Column(modifier.width(width).clickable(onClick = onClick)) {
        Box(
            Modifier
                .width(width)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(StreamingRadius.Card))
                .background(IptvColors.Surface)
                .border(1.dp, IptvColors.Divider, RoundedCornerShape(StreamingRadius.Card)),
            contentAlignment = Alignment.Center
        ) {
            if (channel.logo.isNotBlank()) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Outlined.Movie,
                    contentDescription = null,
                    tint = IptvColors.TextTertiary
                )
            }
            Box(
                Modifier.fillMaxSize().background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.66f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.80f)
                    )
                )
            )
            Text(
                channel.name,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
            )
            if (quality.isNotBlank()) {
                Text(
                    quality,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = .68f), RoundedCornerShape(4.dp))
                        .border(1.dp, Color.White.copy(alpha = .72f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}
