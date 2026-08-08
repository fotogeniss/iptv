package com.prelude.iptv.ui.tv.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.LibraryDestination
import com.prelude.iptv.ui.components.library.LibraryArtwork
import com.prelude.iptv.ui.components.library.LibraryCardBadges
import com.prelude.iptv.ui.components.library.LibraryCardProgress
import com.prelude.iptv.ui.components.library.libraryDescription
import com.prelude.iptv.ui.components.library.libraryMetaLine
import com.prelude.iptv.ui.components.library.libraryTitle
import com.prelude.iptv.ui.rememberInitialFocus
import androidx.compose.animation.core.tween
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TvLibraryCard(
    channel: Channel,
    poster: Boolean,
    progress: Float?,
    favorite: Boolean,
    managementMode: Boolean,
    initialFocus: Boolean,
    onFocused: () -> Unit,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) motionScale(Motion.TvFocusScale) else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "libraryCardScale"
    )
    val focusRequester = rememberInitialFocus(enabled = initialFocus, key = channel)
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier
            .then(if (initialFocus) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged {
                focused = it.isFocused || it.hasFocus
                if (focused) onFocused()
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (focused) 22.dp.toPx() else 0f
                this.shape = shape
                clip = false
                alpha = if (focused) 1f else .90f
            }
            .combinedClickable(
                onClick = { if (managementMode) onRemove() else onOpen() },
                onLongClick = onRemove
            )
            .focusable()
    ) {
        Box(
            Modifier.fillMaxWidth()
                .then(if (poster) Modifier.aspectRatio(2f / 3f) else Modifier.aspectRatio(16f / 9f))
                .clip(shape).background(IptvColors.Surface)
        ) {
            LibraryArtwork(channel, modifier = Modifier.fillMaxSize())
            LibraryCardBadges(channel, favorite, managementMode && focused)
            LibraryCardProgress(progress)
            if (focused) {
                Box(
                    Modifier.align(Alignment.Center).size(46.dp).clip(RoundedCornerShape(99.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (managementMode) Icons.Default.Delete else Icons.Default.PlayArrow,
                        null,
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            libraryTitle(channel),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            libraryMetaLine(channel),
            color = IptvColors.TextSecondary,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun TvLibraryInfoPanel(
    channel: Channel?,
    meta: TmdbClient.Meta?,
    progress: Float?,
    destination: LibraryDestination,
    favorite: Boolean,
    managementMode: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xD90E1115)).padding(16.dp)
    ) {
        if (channel == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Δεν υπάρχει περιεχόμενο", color = IptvColors.TextSecondary)
            }
            return@Column
        }
        Box(Modifier.fillMaxWidth().height(158.dp).clip(RoundedCornerShape(12.dp))) {
            LibraryArtwork(channel, meta?.backdrop.orEmpty(), Modifier.fillMaxSize())
            LibraryCardProgress(progress)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            when (destination) {
                LibraryDestination.MY_LIST -> "ΣΤΗ ΛΙΣΤΑ ΜΟΥ"
                LibraryDestination.CONTINUE_WATCHING -> "ΣΥΝΕΧΙΣΕ ΝΑ ΒΛΕΠΕΙΣ"
                LibraryDestination.HISTORY -> "ΠΡΟΣΦΑΤΑ ΠΡΟΒΛΗΘΗΚΕ"
                LibraryDestination.SEARCH -> "ΒΙΒΛΙΟΘΗΚΗ"
            },
            color = IptvColors.Success,
            fontSize = 9.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(6.dp))
        Text(
            libraryTitle(channel),
            color = Color.White,
            fontSize = 23.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(7.dp))
        Text(libraryMetaLine(channel, meta), color = Color(0xFFD3D7DC), fontSize = 11.sp, maxLines = 2)
        Spacer(Modifier.height(10.dp))
        Text(
            libraryDescription(channel, meta),
            color = IptvColors.TextSecondary,
            fontSize = 11.sp,
            lineHeight = 17.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvLibraryAction(
                label = if (progress != null) "Συνέχεια" else "Αναπαραγωγή",
                icon = Icons.Default.PlayArrow,
                primary = true,
                onClick = onOpen,
                modifier = Modifier.weight(1f)
            )
            TvLibraryAction(
                label = if (managementMode) "Αφαίρεση" else if (favorite) "✓" else "+",
                icon = when {
                    managementMode -> Icons.Default.Delete
                    favorite -> Icons.Default.Check
                    else -> Icons.Default.Add
                },
                primary = false,
                onClick = if (managementMode) onRemove else onToggleFavorite,
                modifier = Modifier.width(104.dp)
            )
        }
    }
}

@Composable
private fun TvLibraryAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.height(42.dp).clip(RoundedCornerShape(9.dp))
            .background(if (primary) Color.White else Color.White.copy(alpha = .10f))
            .clickable(onClick = onClick).focusable()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = if (primary) Color.Black else Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = if (primary) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}
