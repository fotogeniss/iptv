package com.prelude.iptv.ui.mobile.live

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.prelude.iptv.R
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.EpgManager
import com.prelude.iptv.ui.AppFonts
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.StreamingProgress
import com.prelude.iptv.ui.components.epg.epgProgress
import com.prelude.iptv.ui.components.epg.epgTime
import com.prelude.iptv.ui.icons.filled.PlayArrow
import com.prelude.iptv.ui.icons.filled.Star
import com.prelude.iptv.ui.icons.filled.StarBorder

/*
 * Δομικά στοιχεία της νέας Live (prototypes/redesign_live.html): οδηγός καναλιών,
 * όχι βιτρίνα. Κάθε κανάλι δείχνει τι παίζει, πρόοδο και τι ακολουθεί.
 */

/** Τι παίζει τώρα και μετά. Δοκιμάζει τα ids που χρησιμοποιούν οι πάροχοι. */
internal fun liveNowNext(channel: Channel, nowMs: Long): Pair<EpgManager.Prog?, EpgManager.Prog?> {
    if (!EpgManager.isLoaded()) return null to null
    for (id in sequenceOf(channel.tvgId, channel.chId, channel.streamId, channel.name)) {
        if (id.isBlank() || !EpgManager.hasChannel(id)) continue
        return EpgManager.nowNext(id, nowMs)
    }
    return null to null
}

/** Τα επόμενα [count] προγράμματα μετά από αυτό που παίζει τώρα. */
internal fun liveUpcoming(channel: Channel, count: Int, nowMs: Long): List<EpgManager.Prog> {
    if (!EpgManager.isLoaded()) return emptyList()
    for (id in sequenceOf(channel.tvgId, channel.chId, channel.streamId, channel.name)) {
        if (id.isBlank() || !EpgManager.hasChannel(id)) continue
        return EpgManager.upcoming(id, count + 1, nowMs).filter { it.startMs > nowMs }.take(count)
    }
    return emptyList()
}

private val qualityTokens = setOf("HD", "FHD", "UHD", "SD", "4K", "HEVC", "H265", "1080P", "720P", "TV")

/** Μονόγραμμα για κανάλι χωρίς λογότυπο: «ΕΡΤ Σπορ 1 HD» → «ΕΣ1», «Alpha» → «ALP». */
internal fun channelMonogram(name: String): String {
    val words = name.replace(Regex("[^\\p{L}\\p{N}+ ]"), " ")
        .split(' ')
        .filter { it.isNotBlank() && it.uppercase() !in qualityTokens }
    if (words.isEmpty()) return name.take(2).uppercase()
    if (words.size == 1) return words[0].take(3).uppercase()
    // Λέξη που είναι ήδη συντομογραφία (ΕΡΤ, CNN) κρατιέται ολόκληρη αν είναι πρώτη.
    val first = words[0]
    if (first.length in 2..4 && first == first.uppercase()) return first
    return words.take(3).joinToString("") { it.take(1) }.uppercase()
}

/** Δύο τόνοι από το όνομα του καναλιού, ώστε κάθε κανάλι να έχει το δικό του χρώμα. */
private fun monogramBrush(name: String): Brush {
    val hue = ((name.hashCode() and Int.MAX_VALUE) % 360).toFloat()
    return Brush.linearGradient(
        listOf(Color.hsv(hue, 0.62f, 0.52f), Color.hsv(hue, 0.65f, 0.20f))
    )
}

/** Λογότυπο καναλιού ή, αν λείπει, έγχρωμο μονόγραμμα. Ποτέ ίδιο γκρι κουτί. */
@Composable
internal fun ChannelBadge(channel: Channel, size: Dp, modifier: Modifier = Modifier, corner: Dp = 13.dp) {
    val shape = RoundedCornerShape(corner)
    if (channel.logo.isNotBlank()) {
        Box(
            modifier.size(size).clip(shape).background(IptvColors.SurfaceSelected),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = channel.logo,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(size / 9)
            )
        }
    } else {
        Box(
            modifier.size(size).clip(shape).background(monogramBrush(channel.name)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                channelMonogram(channel.name),
                color = Color.White,
                fontFamily = AppFonts.Display,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.31f).sp,
                maxLines = 1,
            )
        }
    }
}

/** Κάρτα «Συνέχισε»: ένα από τα τελευταία κανάλια, με ό,τι παίζει αυτή τη στιγμή. */
@Composable
internal fun LiveContinueCard(channel: Channel, nowMs: Long, onPlay: () -> Unit) {
    val (now, _) = remember(channel, nowMs / 60_000L) { liveNowNext(channel, nowMs) }
    Column(
        Modifier
            .width(136.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(IptvColors.Surface)
            .border(1.dp, IptvColors.Divider, RoundedCornerShape(16.dp))
            .clickable(onClick = onPlay)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box {
            ChannelBadge(channel, 58.dp, Modifier.fillMaxWidth(), corner = 12.dp)
            Box(
                Modifier.align(Alignment.BottomEnd).padding(6.dp).size(24.dp)
                    .background(IptvColors.Background.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Column {
            Text(channel.name, color = IptvColors.TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                now?.title ?: channel.group,
                color = IptvColors.TextTertiary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (now != null) StreamingProgress(epgProgress(now, nowMs), Modifier.fillMaxWidth())
    }
}

/** Μία γραμμή του οδηγού: αριθμός, λογότυπο, κανάλι, τώρα + πρόοδος, μετά, αγαπημένο. */
@Composable
internal fun LiveGuideRow(
    number: Int,
    channel: Channel,
    nowMs: Long,
    playing: Boolean,
    favorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: (() -> Unit)?,
) {
    val (now, next) = remember(channel, nowMs / 60_000L) { liveNowNext(channel, nowMs) }
    Box(
        Modifier
            .fillMaxWidth()
            .then(
                if (playing) Modifier.background(
                    Brush.horizontalGradient(listOf(IptvColors.Primary.copy(alpha = 0.12f), Color.Transparent))
                ) else Modifier
            )
            .clickable(onClick = onPlay)
    ) {
        if (playing) {
            Box(Modifier.matchParentSize().padding(vertical = 12.dp)) {
                Box(Modifier.width(2.dp).fillMaxHeight().background(IptvColors.Primary, RoundedCornerShape(2.dp)))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                number.toString(),
                color = IptvColors.TextTertiary,
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier.width(30.dp).padding(end = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
            ChannelBadge(channel, 46.dp)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(channel.name, color = IptvColors.TextTertiary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    now?.title ?: channel.group.ifBlank { channel.name },
                    color = IptvColors.TextPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (now != null) {
                    StreamingProgress(epgProgress(now, nowMs), Modifier.fillMaxWidth().padding(vertical = 2.dp))
                }
                if (next != null) {
                    Text(
                        stringResource(R.string.live_time_and_remaining, epgTime(next.startMs), next.title),
                        color = IptvColors.TextTertiary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (playing) {
                    PlayingBars()
                } else if (now != null) {
                    Text(epgTime(now.startMs), color = IptvColors.TextTertiary, fontSize = 11.sp, modifier = Modifier.padding(end = 12.dp))
                }
                if (onToggleFavorite != null) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(40.dp)) {
                        Icon(
                            if (favorite) Icons.Default.Star else Icons.Default.StarBorder,
                            stringResource(R.string.live_favorite),
                            tint = if (favorite) IptvColors.Warning else IptvColors.TextTertiary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Τρεις κόκκινες μπάρες: «αυτό παίζει τώρα». */
@Composable
private fun PlayingBars() {
    Row(
        Modifier.padding(end = 12.dp, top = 2.dp).height(12.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(7.dp, 12.dp, 9.dp).forEach { h ->
            Box(Modifier.width(3.dp).height(h).background(IptvColors.Glow, RoundedCornerShape(1.dp)))
        }
    }
}

/** Καρτέλα κατηγορίας (chip) με πλήθος. */
@Composable
internal fun LiveCategoryTab(
    label: String,
    count: Int?,
    selected: Boolean,
    leading: (@Composable () -> Unit)? = null,
    dashed: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(if (selected) IptvColors.TextPrimary else if (dashed) Color.Transparent else IptvColors.Surface)
            .border(1.dp, if (selected) IptvColors.TextPrimary else IptvColors.DividerStrong, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        leading?.invoke()
        Text(
            label,
            color = if (selected) IptvColors.Background else IptvColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        if (count != null) {
            Text(
                count.toString(),
                color = if (selected) IptvColors.Background.copy(alpha = 0.6f) else IptvColors.TextTertiary,
                fontSize = 11.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun LiveSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        java.text.Normalizer.normalize(text.uppercase(), java.text.Normalizer.Form.NFD).replace("́", ""),
        color = IptvColors.TextTertiary,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        modifier = modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 8.dp)
    )
}
