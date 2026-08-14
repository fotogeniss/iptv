package com.prelude.iptv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.BuildConfig
import com.prelude.iptv.player.PlaybackEngine
import com.prelude.iptv.R
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.mobile.navigation.PremiumMobileMiniPlayerHeight

/** Ύψος της μαζεμένης μπάρας. Το βίντεο μέσα της είναι 16:9, άρα 102dp πλάτος. */
internal val MiniPlayerHeight = PremiumMobileMiniPlayerHeight

/**
 * Ο μαζεμένος player: μια λωρίδα στο κάτω μέρος που **συνεχίζει να παίζει**.
 *
 * ΤΟ ΚΡΙΣΙΜΟ ΔΕΝ ΕΙΝΑΙ ΕΔΩ: είναι ότι το [MobilePlaybackOverlay] ΔΕΝ βγαίνει από
 * τη σύνθεση όταν μαζεύεται. Η [PlaybackEngine] ζει σε ένα `remember` μέσα του, με
 * `DisposableEffect` που την ελευθερώνει στο dispose — αν το μάζεμα γινόταν με
 * αλλαγή της κατάστασης του γονέα (target = null), η μηχανή θα ελευθερωνόταν και ο
 * ήχος θα κοβόταν. Το μάζεμα είναι **αλλαγή διάταξης**, όχι αλλαγή περιεχομένου.
 *
 * ΓΙΑ ΤΟΝ ΙΔΙΟ ΛΟΓΟ Η ΕΠΙΦΑΝΕΙΑ ΜΕΝΕΙ ΖΩΝΤΑΝΗ: δείχνουμε το πραγματικό βίντεο και
 * όχι μια αφίσα. Μια στατική εικόνα θα έλεγε στον χρήστη «σταμάτησε», ενώ ο ήχος
 * συνεχίζει — και αυτή η αντίφαση είναι πιο ενοχλητική από μικρή εικόνα.
 *
 * ΔΕΝ ΜΠΛΟΚΑΡΕΙ ΤΗΝ ΟΘΟΝΗ: ο γονέας είναι διάφανο [Box] χωρίς χειρονομίες, οπότε
 * τα αγγίγματα περνούν από πάνω του στη λίστα από κάτω. Αυτό είναι όλο το νόημα —
 * ψάχνεις κάτι άλλο ενώ παίζει.
 */
@Composable
internal fun MobileMiniPlayer(
    engine: PlaybackEngine,
    /**
     * Η ΙΔΙΑ επιφάνεια που έδειχνε την πλήρη οθόνη, μεταφερμένη εδώ.
     *
     * Δίνεται από τα έξω και δεν φτιάχνεται εδώ, γιατί δεύτερη επιφάνεια πάνω
     * στον ίδιο player σημαίνει δύο διεκδικητές της μίας εξόδου βίντεο. Δες το
     * `movableContentOf` στο [MobilePlaybackOverlay].
     */
    video: @Composable (Modifier) -> Unit,
    playing: Boolean,
    title: String,
    subtitle: String,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .clip(
                RoundedCornerShape(
                    topStart = 13.dp,
                    topEnd = 13.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp,
                )
            )
            .background(Color.Black.copy(alpha = .86f))
            // Το άγγιγμα ΟΠΟΥΔΗΠΟΤΕ στη λωρίδα ξαναμεγαλώνει. Τα δύο κουμπιά
            // δεξιά έχουν δικές τους περιοχές που το προλαβαίνουν.
            .pointerInput(Unit) { detectTapGestures { onExpand() } },
        verticalAlignment = Alignment.CenterVertically
    ) {
        var videoBoxSize by remember { mutableStateOf(IntSize.Zero) }
        Box(
            Modifier
                .width(MiniPlayerHeight * 16f / 9f)
                .fillMaxHeight()
                .background(Color.Black)
                .onSizeChanged { videoBoxSize = it },
            contentAlignment = Alignment.Center
        ) {
            // Και μαζεμένος, ο χρήστης ΒΛΕΠΕΙ. Δεν είναι αφίσα: είναι η ίδια
            // ζωντανή επιφάνεια, απλώς σε 121x68dp.
            video(Modifier.fillMaxSize())
            // ΠΡΟΣΩΡΙΝΟ ΔΙΑΓΝΩΣΤΙΚΟ, ΜΟΝΟ ΣΤΟ QA BUILD.
            //
            // Η λωρίδα βγάζει ήχο χωρίς εικόνα και το ιστορικό δεν έχει τι να
            // δείξει: ο κώδικας είναι ίδιος με το πρώτο commit. Αυτό μετατρέπει
            // μια φωτογραφία της οθόνης σε μέτρηση — ποια μηχανή παίζει, πόσο
            // μεγάλη είναι η επιφάνεια, ποια επιφάνεια είναι δεμένη, και κυρίως
            // αν έχει έρθει έστω ένα καρέ σε αυτήν. Φεύγει μόλις βρεθεί η αιτία.
            if (BuildConfig.PREMIUM_QA_OVERRIDE) {
                val renderer by engine.renderer.collectAsState()
                val engineState by engine.state.collectAsState()
                Text(
                    listOf(
                        "$renderer f=${engineState.renderedFrames}",
                        "${videoBoxSize.width}x${videoBoxSize.height}",
                        "${engine.attachedSurfaceLabel()} ar=${"%.2f".format(engineState.videoAspect)}",
                    ).joinToString("\n"),
                    color = Color(0xFFFFD54F),
                    fontSize = 7.sp,
                    lineHeight = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopStart).padding(horizontal = 3.dp),
                )
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    color = IptvColors.TextTertiary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        MiniAction(
            if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
            stringResource(if (playing) R.string.player_pause else R.string.player_play)
        ) { engine.togglePlay() }
        MiniAction(Icons.Default.Close, stringResource(R.string.player_close), onClick = onClose)
        Spacer(Modifier.width(4.dp))
    }
}

@Composable
private fun MiniAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(42.dp)
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}
