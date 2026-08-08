package com.prelude.iptv.ui.route

import android.content.*
import android.os.*
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.*
import androidx.activity.compose.*
import androidx.activity.result.contract.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.*
import coil.compose.*
import com.prelude.iptv.*
import com.prelude.iptv.data.*
import com.prelude.iptv.ui.*
import com.prelude.iptv.ui.components.library.*
import com.prelude.iptv.ui.design.*
import com.prelude.iptv.ui.mobile.navigation.premiumMobileNavigationContentPadding
import kotlinx.coroutines.*

@Composable
internal fun ContinueWatchingRow(
    items: List<Pair<Channel, Float>>,
    onPlay: (Channel) -> Unit
) {
    Column(Modifier.padding(bottom = 6.dp)) {
        Text(
            stringResource(R.string.home_section_continue), color = TextHi, fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.first.name + it.first.url + it.first.seriesId }) { (ch, progress) ->
                Column(
                    Modifier.width(if (isTvDevice()) 120.dp else 138.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .tvFocus(RoundedCornerShape(12.dp))
                        .clickable { onPlay(ch) }
                ) {
                    Box(
                        Modifier.height(if (isTvDevice()) 160.dp else 184.dp).fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)).background(BgElev2)
                    ) {
                        if (ch.logo.isNotEmpty()) {
                            AsyncImage(
                                model = ch.logo, contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.Movie, null, tint = TextLo,
                                modifier = Modifier.align(Alignment.Center).size(32.dp))
                        }
                        Icon(
                            Icons.Default.PlayArrow, null, tint = Color.White,
                            modifier = Modifier.align(Alignment.Center).size(36.dp)
                                .background(Color(0x66000000), CircleShape).padding(4.dp)
                        )
                        // μπάρα προόδου πάνω στο poster — «πού έμεινα» με μια ματιά
                        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth()
                            .height(4.dp).background(Color(0x66000000)))
                        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(progress)
                            .height(4.dp).background(Accent))
                    }
                    Text(
                        ch.name, color = TextMid, fontSize = 11.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp)
                    )
                }
            }
        }
    }
}

/** Mobile-only escape hatch: an empty section must never hide refresh/settings. */
@Composable
private fun EmptyStateRecoveryActions(
    onRefresh: (() -> Unit)?,
    onOpenSettings: (() -> Unit)?,
) {
    if (isTvDevice() || (onRefresh == null && onOpenSettings == null)) return
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        onRefresh?.let { refresh ->
            OutlinedButton(
                onClick = refresh,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Line),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextHi),
            ) { Text(stringResource(R.string.browse_refresh)) }
        }
        onOpenSettings?.let { openSettings ->
            TextButton(onClick = openSettings) { Text(stringResource(R.string.nav_settings)) }
        }
    }
}

/**
 * Άδεια οθόνη που ΕΞΗΓΕΙ γιατί είναι άδεια και δίνει την επόμενη κίνηση.
 * Πριν: σκέτο «Δεν υπάρχουν στοιχεία εδώ» για 4 διαφορετικές αιτίες (δεν
 * φορτώθηκε ποτέ / φίλτρο χωρίς αποτελέσματα / σφάλμα / Άκυρο πρώτη φορά)
 * — ο χρήστης έμενε να κοιτάει κενό χωρίς να ξέρει τι να κάνει.
 */
@Composable
internal fun EmptyState(
    hasLoaded: Boolean,
    isError: Boolean,
    message: String,
    onLoad: () -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    val f = rememberInitialFocus(enabled = isTvDevice())
    Box(
        Modifier.fillMaxSize().padding(
            bottom = if (isTvDevice()) 0.dp else premiumMobileNavigationContentPadding()
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            when {
                hasLoaded -> {
                    // υπάρχουν δεδομένα — απλά το φίλτρο/group δεν πιάνει τίποτα
                    Icon(Icons.Default.SearchOff, null, tint = BgElev2, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.browse_empty_filter), color = TextMid, fontSize = 15.sp)
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = onClearFilters,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Line),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextHi),
                        modifier = Modifier.focusRequester(f).tvFocus(RoundedCornerShape(12.dp))
                    ) { Text(stringResource(R.string.browse_clear_filters)) }
                }
                isError -> {
                    Icon(Icons.Outlined.Info, null, tint = BgElev2, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.browse_generic_error), color = TextMid, fontSize = 15.sp)
                    if (message.isNotBlank())
                        Text(message, color = TextLo, fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onLoad,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        modifier = Modifier.focusRequester(f).tvFocus(RoundedCornerShape(12.dp), tint = false)
                    ) { Text(stringResource(R.string.browse_try_again), fontWeight = FontWeight.Bold) }
                }
                else -> {
                    // δεν έχει φορτωθεί ΠΟΤΕ (ή πάτησε «Άκυρο» την πρώτη φορά)
                    Icon(Icons.Outlined.LiveTv, null, tint = BgElev2, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.browse_not_loaded), color = TextMid, fontSize = 15.sp)
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onLoad,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        modifier = Modifier.focusRequester(f).tvFocus(RoundedCornerShape(12.dp), tint = false)
                    ) { Text(stringResource(R.string.browse_load_now), fontWeight = FontWeight.Bold) }
                }
            }
            if (!hasLoaded) EmptyStateRecoveryActions(onRefresh, onOpenSettings)
        }
    }
}

/**
 * Ερώτηση πριν τη φόρτωση: όλα ή επιλογή κατηγοριών;
 * Φτιαγμένο με μεγάλες επιλογές ώστε να δουλεύει και με τηλεχειριστήριο.
 */
// Το AskLoadTypeDialog («Δεν υπάρχουν Χ εδώ — να τα φορτώσω;») αφαιρέθηκε:
// πατώντας την ενότητα, η φόρτωση ξεκινά κατευθείαν — ένα βήμα λιγότερο.


@Composable
internal fun RefreshModeDialog(
    contentType: String,
    onExisting: () -> Unit,
    onChooseGroups: () -> Unit,
    onCancel: () -> Unit
) {
    val first = rememberInitialFocus(key = contentType)
    val section = when (contentType) {
        "vod" -> "Ταινίες"
        "series" -> "Σειρές"
        else -> "Live TV"
    }

    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = BgElev2,
        title = { Text("Ανανέωση · $section", color = TextHi) },
        text = {
            Column {
                Text(
                    "Διάλεξε αν θα κρατηθούν τα groups που έχεις ήδη επιλέξει ή αν θέλεις να κατέβει ξανά η διαθέσιμη λίστα groups.",
                    color = TextMid,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(16.dp))
                LoadModeOption(
                    title = "Ανανέωση τρέχουσας επιλογής",
                    subtitle = "Κρατά ακριβώς τα groups που χρησιμοποιείς τώρα",
                    icon = Icons.Default.Refresh,
                    modifier = Modifier.focusRequester(first),
                    testTag = "refresh-mode-existing",
                    onClick = onExisting
                )
                Spacer(Modifier.height(10.dp))
                LoadModeOption(
                    title = "Ανανέωση + επιλογή νέων groups",
                    subtitle = "Φέρνει φρέσκια λίστα για Live TV, Ταινίες ή Σειρές",
                    icon = Icons.Default.PlaylistAdd,
                    testTag = "refresh-mode-new-groups",
                    onClick = onChooseGroups
                )
            }
        },
        confirmButton = {},
        dismissButton = { TvDialogTextButton(label = "Άκυρο", color = TextMid, onClick = onCancel) }
    )
}

@Composable
internal fun LoadModeDialog(
    count: Int,
    onAll: () -> Unit,
    onChoose: () -> Unit,
    onCancel: () -> Unit
) {
    val focusAll = rememberInitialFocus(key = count)

    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = BgElev2,
        title = { Text("Τι να φορτώσω;", color = TextHi) },
        text = {
            Column {
                Text(
                    "Βρέθηκαν $count κατηγορίες. Τα δεδομένα της ενότητας κατεβαίνουν φρέσκα κάθε φορά.",
                    color = TextMid, fontSize = 13.sp, lineHeight = 18.sp
                )
                Spacer(Modifier.height(16.dp))

                LoadModeOption(
                    title = "Όλες οι κατηγορίες",
                    subtitle = "Μόνο για την τρέχουσα ενότητα",
                    icon = Icons.Default.SelectAll,
                    modifier = Modifier.focusRequester(focusAll),
                    testTag = "load-mode-all",
                    onClick = onAll
                )
                Spacer(Modifier.height(10.dp))
                LoadModeOption(
                    title = "Θέλω να επιλέξω",
                    subtitle = "Διάλεξε κατηγορίες για την τρέχουσα ενότητα",
                    icon = Icons.Default.Checklist,
                    testTag = "load-mode-choose",
                    onClick = onChoose
                )
            }
        },
        confirmButton = {},
        dismissButton = { TvDialogTextButton(label = "Άκυρο", color = TextMid, onClick = onCancel) }
    )
}

@Composable
internal fun LoadModeOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgElev)
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .tvFocus(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = AccentSoft, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = TextHi, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextLo, fontSize = 12.sp)
        }
    }
}
