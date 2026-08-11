package com.prelude.iptv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared chrome for the streaming UI. These components intentionally avoid red
 * backgrounds and glows. Red is reserved for semantic accents such as LIVE and
 * playback progress; navigation uses neutral surfaces and white selection.
 */
data class StreamingSegment<T>(
    val value: T,
    val label: String
)

@Composable
fun <T> StreamingSegmentedControl(
    items: List<StreamingSegment<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    tv: Boolean = isTvDevice()
) {
    if (tv) {
        Row(
            modifier
                .fillMaxWidth()
                .background(IptvColors.BackgroundRaised, RoundedCornerShape(12.dp))
                .border(1.dp, IptvColors.Divider, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                val active = item.value == selected
                Box(
                    Modifier
                        .weight(1f)
                        .height(46.dp)
                        .background(if (active) Color.White else Color.Transparent, RoundedCornerShape(9.dp))
                        .tvFocus(RoundedCornerShape(9.dp), tint = false)
                        .clickable { onSelect(item.value) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.label, color = if (active) Color.Black else IptvColors.TextSecondary,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }
    } else {
        Row(
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                val active = item.value == selected
                Box(
                    Modifier
                        .height(40.dp)
                        .background(if (active) Color.White else IptvColors.Surface, RoundedCornerShape(99.dp))
                        .border(1.dp, if (active) Color.White else IptvColors.Divider, RoundedCornerShape(99.dp))
                        .clickable { onSelect(item.value) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.label, color = if (active) Color.Black else IptvColors.TextSecondary,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

data class StreamingNavItem<T>(
    val value: T?,
    val label: String,
    val icon: ImageVector,
    val action: (() -> Unit)? = null
)

@Composable
fun <T> StreamingBottomNavigation(
    items: List<StreamingNavItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xF20D0D0F),
        tonalElevation = 0.dp,
        shadowElevation = 16.dp,
        modifier = modifier.fillMaxWidth().navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val active = item.value != null && item.value == selected
                Column(
                    Modifier
                        .weight(1f)
                        .background(
                            if (active) Color.White.copy(alpha = 0.08f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        // ΟΡΑΤΟ FOCUS ΓΙΑ ΤΗΛΕΧΕΙΡΙΣΤΗΡΙΟ.
                        //
                        // Το `clickable` κάνει το στοιχείο focusable, οπότε το
                        // D-pad ΕΦΤΑΝΕ εδώ και πριν — αλλά τίποτα δεν άλλαζε
                        // στην οθόνη, οπότε ο χρήστης δεν είχε τρόπο να ξέρει
                        // πού βρίσκεται. Το πρόβλημα υπήρχε σε ΚΑΙ ΤΑ ΠΕΝΤΕ
                        // στοιχεία της μπάρας από την αρχή· απλώς δεν φαινόταν
                        // όσο υπήρχε το κουμπί «＋ Νέα πηγή» στην κεφαλίδα, που
                        // είχε δικό του tvFocus. Μόλις εκείνο αφαιρέθηκε, η
                        // μπάρα έγινε ο μόνος δρόμος και το κενό έγινε ορατό.
                        //
                        // ΠΡΙΝ το `clickable`, όπως παντού αλλού στο app: το
                        // tvFocus δεν κάνει το στοιχείο focusable, μόνο ζωγραφίζει
                        // — πρέπει να δει το focus event που παράγει το clickable.
                        .tvFocus(RoundedCornerShape(10.dp), tint = false, scale = false)
                        .clickable {
                            item.action?.invoke() ?: item.value?.let(onSelect)
                        }
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (active) Color.White else IptvColors.TextTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = item.label,
                        color = if (active) Color.White else IptvColors.TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun StreamingScreenHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    tv: Boolean = isTvDevice()
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = IptvColors.TextPrimary,
                style = if (tv) androidx.compose.material3.MaterialTheme.typography.headlineMedium
                else androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    color = IptvColors.TextSecondary,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}
