package com.prelude.iptv.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.player.PlaybackEngine
import com.prelude.iptv.R
import com.prelude.iptv.ui.localization.badgeLabelRes

/** Το κόκκινο της μπάρας. Ίδιο με το λογότυπο — δεν είναι δεύτερο κόκκινο. */
private val SCRUB_RED = Color(0xFFE50914)

/**
 * Τα χειριστήρια του player στο κινητό, στη διάταξη του YouTube.
 *
 * ΓΙΑΤΙ ΞΕΧΩΡΙΣΤΟ ΑΡΧΕΙΟ: το [MobilePlaybackOverlay] κρατά τον κύκλο ζωής της
 * μηχανής — άνοιγμα ροής, αποθήκευση θέσης, απελευθέρωση. Αυτά είναι λάθος που
 * κοστίζει (διαρροή μνήμης, χαμένη πρόοδος). Τα χειριστήρια είναι εμφάνιση, και
 * αλλάζουν δέκα φορές πιο συχνά. Ανακατεμένα, κάθε αλλαγή στιλ ακουμπούσε αρχείο
 * που περιέχει διαχείριση πόρων.
 *
 * Η ΔΙΑΤΑΞΗ, όπως στο YouTube σε κατακόρυφη οθόνη:
 *
 * ```
 *  ⌄                                    cc  ⚙
 *
 *                     ▶
 *
 *  0:19 / 3:48         FHD  AUTO  ⏱  ⛶
 *  ━━━━━●───────────────────────────────
 * ```
 *
 * ΧΩΡΙΣ ΒΕΛΑΚΙΑ ΠΡΟΗΓΟΥΜΕΝΟ/ΕΠΟΜΕΝΟ: το κέντρο έχει μόνο play/pause. Στα
 * ζωντανά η αλλαγή καναλιού γίνεται με σύρσιμο αριστερά/δεξιά πάνω στην
 * εικόνα (δες [MobilePlaybackOverlay]), όχι με κουμπί μέσα στα χειριστήρια.
 *
 * Η μπάρα κάθεται στην ΚΑΤΩ ΑΚΜΗ της εικόνας, όχι μέσα σε περιθώριο: εκεί την
 * ψάχνει το δάχτυλο, και εκεί δεν σκεπάζει τίποτα.
 */
@Composable
internal fun MobilePlayerControls(
    engine: PlaybackEngine,
    state: PlaybackEngine.State,
    isLive: Boolean,
    visible: Boolean,
    expanded: Boolean,
    /** Θέση που δείχνει η μπάρα — διαφέρει από τη μηχανή όσο ο χρήστης σέρνει. */
    scrubPositionMs: Long,
    onScrubStart: () -> Unit,
    onScrub: (Long) -> Unit,
    onScrubEnd: () -> Unit,
    onClose: () -> Unit,
    onToggleExpanded: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenSettings: () -> Unit,
    aspectMode: AspectMode,
    onOpenAspectRatio: () -> Unit,
    onOpenSleep: () -> Unit,
    onInteract: () -> Unit,
    /** null = δεν υπάρχει επόμενο/προηγούμενο· το κουμπί ξεθωριάζει αντί να λείπει. */
    onPlayNext: (() -> Unit)? = null,
    onPlayPrevious: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(Modifier.fillMaxSize()) {
            // ---- ΠΑΝΩ ----
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = .55f),
                            1f to Color.Transparent
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Βελάκι ΚΑΤΩ και όχι «πίσω»: η κίνηση είναι «μάζεψε τον player»,
                // και το βέλος δείχνει προς τα πού φεύγει.
                ChromeIcon(Icons.Default.KeyboardArrowDown, stringResource(R.string.player_close), size = 24.dp) {
                    onClose()
                }
                Spacer(Modifier.weight(1f))
                ChromeIcon(Icons.Default.ClosedCaption, stringResource(R.string.player_subtitles)) {
                    onInteract(); onOpenSubtitles()
                }
                ChromeIcon(Icons.Default.Settings, stringResource(R.string.player_settings)) {
                    onInteract(); onOpenSettings()
                }
            }

            // ---- ΚΕΝΤΡΟ ----
            // Μόνο play/pause. Τα βελάκια προηγούμενο/επόμενο έφυγαν από εδώ σε
            // όλους τους τύπους περιεχομένου (ταινίες, ζωντανά, σειρές)· η αλλαγή
            // καναλιού στα ζωντανά γίνεται πλέον με σύρσιμο αριστερά/δεξιά.
            Icon(
                if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(
                    if (state.playing) R.string.player_pause else R.string.player_play
                ),
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { engine.togglePlay(); onInteract() }
                    }
            )

            // ---- ΚΑΤΩ ----
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black.copy(alpha = .70f)
                        )
                    )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLive) {
                        // Ζωντανή ροή: δεν υπάρχει «0:19 από 3:48». Η κόκκινη
                        // κουκκίδα λέει αυτό που πρέπει — παίζει τώρα.
                        Box(
                            Modifier.size(8.dp).clip(CircleShape).background(SCRUB_RED)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            stringResource(R.string.player_live),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = .8.sp
                        )
                    } else {
                        Text(
                            buildString {
                                append(mobileClock(scrubPositionMs))
                                append(" / ")
                                append(mobileClock(state.durationMs))
                            },
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    QualityBadge(state.quality)
                    AspectRatioBadge(aspectMode) {
                        onInteract()
                        onOpenAspectRatio()
                    }
                    // Στη θέση του «watch later» του YouTube: χρονοδιακόπτης ύπνου.
                    // Ένα κουμπί που μοιάζει με του YouTube αλλά δεν κάνει τίποτα
                    // θα ήταν χειρότερο από κανένα κουμπί.
                    ChromeIcon(Icons.Default.Bedtime, stringResource(R.string.player_sleep_timer), size = 18.dp) {
                        onInteract(); onOpenSleep()
                    }
                    ChromeIcon(
                        if (expanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        stringResource(
                            if (expanded) R.string.player_exit_fullscreen else R.string.player_fullscreen
                        ),
                        size = 22.dp
                    ) { onToggleExpanded() }
                }

                if (isLive || state.durationMs <= 0) {
                    Spacer(Modifier.height(10.dp))
                } else {
                    Scrubber(
                        positionMs = scrubPositionMs,
                        durationMs = state.durationMs,
                        onScrubStart = onScrubStart,
                        onScrub = onScrub,
                        onScrubEnd = onScrubEnd,
                        onInteract = onInteract,
                    )
                }
            }
        }
    }
}

/** Μικρή ένδειξη/κουμπί αναλογίας ακριβώς δίπλα στην ποιότητα. */
@Composable
private fun AspectRatioBadge(mode: AspectMode, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(start = 3.dp, end = 3.dp)
            .height(25.dp)
            .clip(RoundedCornerShape(5.dp))
            .border(1.dp, Color.White.copy(alpha = .62f), RoundedCornerShape(5.dp))
            .background(Color.White.copy(alpha = .07f))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(width = 13.dp, height = 9.dp)
                .border(1.dp, Color.White, RoundedCornerShape(2.dp))
        )
        Text(
            text = stringResource(mode.badgeLabelRes()),
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

/**
 * Η μπάρα προόδου.
 *
 * ΤΟ TOUCH TARGET ΕΙΝΑΙ ΜΕΓΑΛΥΤΕΡΟ ΑΠΟ ΟΣΟ ΦΑΙΝΕΤΑΙ: η γραμμή έχει 2dp ύψος, αλλά η
 * περιοχή που δέχεται το δάχτυλο 26dp. Μια μπάρα 3dp είναι αδύνατο να πιαστεί —
 * και αν προσπαθήσεις να τη μεγαλώσεις για να πιάνεται, γίνεται άσχημη.
 */
@Composable
private fun Scrubber(
    positionMs: Long,
    durationMs: Long,
    onScrubStart: () -> Unit,
    onScrub: (Long) -> Unit,
    onScrubEnd: () -> Unit,
    onInteract: () -> Unit,
) {
    val fraction = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    // ΛΕΠΤΗ ΟΣΟ ΔΕΝ ΤΗΝ ΑΓΓΙΖΕΙΣ, ΕΛΑΧΙΣΤΑ ΠΙΟ ΧΟΝΤΡΗ ΟΤΑΝ ΤΗΝ ΠΑΤΑΣ.
    //
    // Αλλάζουν ΜΟΝΟ το πάχος της γραμμής και η διάμετρος της λαβής. Η περιοχή
    // αφής μένει 26dp: αν μίκραινε μαζί με τη γραμμή, η μπάρα θα γινόταν
    // ωραιότερη και ταυτόχρονα δυσκολότερη να πιαστεί.
    var pressed by remember { mutableStateOf(false) }
    val trackHeight by animateDpAsState(
        targetValue = if (pressed) 3.dp else 1.5f.dp,
        animationSpec = tween(130),
        label = "scrubberTrack",
    )
    val thumbSize by animateDpAsState(
        targetValue = if (pressed) 11.dp else 7.dp,
        animationSpec = tween(130),
        label = "scrubberThumb",
    )
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(26.dp)
            .pointerInput(durationMs) {
                detectTapGestures(
                    // Το πάχος ακολουθεί το ΔΑΧΤΥΛΟ, όχι το αποτέλεσμα: πιάνεται
                    // στο πάτημα και αφήνεται στο σήκωμα, ακόμη κι αν ο χρήστης
                    // τελικά δεν άλλαξε θέση.
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { offset ->
                        onInteract()
                        onScrub(((offset.x / size.width) * durationMs).toLong())
                        onScrubEnd()
                    },
                )
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { pressed = true; onScrubStart(); onInteract() },
                    onDragEnd = { pressed = false; onScrubEnd() },
                    onDragCancel = { pressed = false; onScrubEnd() },
                ) { change, _ ->
                    onScrub(((change.position.x / size.width) * durationMs).toLong())
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val width = maxWidth
        // Υπόλοιπο
        Box(
            Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .background(Color.White.copy(alpha = .30f))
        )
        // Παιγμένο
        Box(
            Modifier
                .width(width * fraction)
                .height(trackHeight)
                .background(SCRUB_RED)
        )
        // Λαβή. Το offset είναι η θέση μείον η μισή λαβή, ώστε το κέντρο του
        // κύκλου να πέφτει ΠΑΝΩ στη θέση και όχι δεξιά της. Η μισή λαβή
        // διαβάζεται από το τρέχον μέγεθος, αλλιώς θα μετατοπιζόταν καθώς
        // μεγαλώνει.
        Box(
            Modifier
                .offset(x = width * fraction - thumbSize / 2)
                .size(thumbSize)
                .clip(CircleShape)
                .background(SCRUB_RED)
        )
    }
}

/** Η ένδειξη ποιότητας, στο ύφος του YouTube: FHD / HD / SD, σε πλαίσιο. */
@Composable
private fun QualityBadge(quality: PlaybackEngine.VideoQuality) {
    val label = qualityLabel(quality.height)
    if (label.isBlank()) return
    Text(
        label,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .border(1.dp, Color.White, RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

/**
 * Ύψος εικόνας -> ετικέτα.
 *
 * Κατώφλια και όχι ακριβείς τιμές: οι πάροχοι στέλνουν 1088 αντί για 1080 και 718
 * αντί για 720 αρκετά συχνά ώστε μια σύγκριση ισότητας να μη δείχνει τίποτα.
 */
internal fun qualityLabel(height: Int): String = when {
    height <= 0 -> ""
    height >= 2000 -> "4K"
    height >= 1400 -> "2K"
    height >= 1000 -> "FHD"
    height >= 700 -> "HD"
    else -> "SD"
}

@Composable
private fun ChromeIcon(
    icon: ImageVector,
    description: String,
    size: androidx.compose.ui.unit.Dp = 20.dp,
    onClick: () -> Unit,
) {
    Box(
        // Το εικονίδιο είναι 20dp, η περιοχή αφής 36dp. Το πρώτο είναι σχέδιο, το
        // δεύτερο είναι το δάχτυλο — και τα δύο μικρότερα από πριν (24dp/44dp),
        // ώστε τα εικονίδια να μοιάζουν πιο λεπτά πάνω στην εικόνα.
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(size))
    }
}

internal fun mobileClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%d:%02d", minutes, seconds)
}
