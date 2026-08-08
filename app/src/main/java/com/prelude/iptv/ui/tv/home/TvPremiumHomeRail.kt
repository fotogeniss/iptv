package com.prelude.iptv.ui.tv.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.rememberPosterArtwork
import com.prelude.iptv.ui.CatalogRailSection
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale

/**
 * TV Home rail — πιστό στο Figma: τίτλος ενότητας + σειρά από ΚΑΘΑΡΕΣ portrait
 * αφίσες (145x207px -> 109x155dp), χωρίς κείμενα πάνω στις κάρτες. Το focus
 * δείχνεται με λευκό περίγραμμα + διακριτικό scale (graphicsLayer — δεν αγγίζει
 * το layout, άρα καμία κάθετη μετακίνηση).
 */
@Composable
internal fun TvPremiumHomeRail(
    section: CatalogRailSection,
    favoriteKeys: Set<String>,
    onFocused: (Channel) -> Unit,
    onOpen: (Channel) -> Unit,
    onLongOpen: (Channel) -> Unit = {},
    firstCardFocus: FocusRequester? = null,
    // Το rail πιάνει ΑΚΡΙΒΩΣ το ύψος του ορατού χώρου: όταν είναι στην κορυφή,
    // δεν περισσεύει τίποτα για να φανεί λωρίδα από τη διπλανή σειρά.
    railHeight: Dp? = null,
    modifier: Modifier = Modifier
) {
    // Κάθε σειρά ξεκινά από την πρώτη της ταινία. (Εμφανίζεται μία σειρά τη φορά,
    // οπότε όταν αλλάζεις ομάδα η νέα ξεκινά πάντα από την αρχή — καμία διαγώνια
    // μετατόπιση για να «προλάβει» το focus την προηγούμενη οριζόντια θέση.)
    val rowState = rememberLazyListState()
    LaunchedEffect(section.id) { rowState.scrollToItem(0) }
    Column(
        modifier
            .fillMaxWidth()
            .then(if (railHeight != null) Modifier.height(railHeight) else Modifier)
            .padding(top = 10.dp, bottom = 6.dp),
        // Top (όχι Center): με κεντράρισμα, κάθε αλλαγή στο περιεχόμενο της σειράς
        // μετατόπιζε κατακόρυφα ΟΛΗ τη σειρά. Από την κορυφή, η θέση είναι σταθερή.
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            section.title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 86.dp, bottom = 8.dp)
        )
        LazyRow(
            state = rowState,
            // ΑΡΙΣΤΕΡΑ 86dp (ευθυγράμμιση με το κείμενο του hero), ΔΕΞΙΑ μόνο 24dp.
            //
            // Το bring-into-view θεωρεί το contentPadding ΕΚΤΟΣ ωφέλιμης περιοχής:
            // με 86dp και δεξιά, μια κάρτα που φαινόταν ολόκληρη θεωρούνταν
            // «κρυμμένη» και η σειρά σκρόλαρε σε ΚΑΘΕ δεξί πάτημα — γι' αυτό
            // μετατοπιζόταν έντονα αριστερά ενώ δεν χρειαζόταν.
            contentPadding = PaddingValues(start = 86.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                section.items,
                key = { index, channel -> "premium-tv:${section.id}:$index:${PlaybackQueue.favKey(channel)}" }
            ) { index, channel ->
                TvPosterCard(
                    channel = channel,
                    progress = section.progress[PlaybackQueue.favKey(channel)],
                    favorite = PlaybackQueue.favKey(channel) in favoriteKeys,
                    onFocused = { onFocused(channel) },
                    onClick = { onOpen(channel) },
                    onLongClick = { onLongOpen(channel) },
                    focusRequester = if (index == 0) firstCardFocus else null
                )
            }
        }
    }
}

/** Καθαρή portrait αφίσα με λευκό focus ring — όπως οι κάρτες του Figma. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TvPosterCard(
    channel: Channel,
    progress: Float?,
    favorite: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    focusRequester: FocusRequester? = null
) {
    val artwork = rememberPosterArtwork(channel)
    var focused by remember { mutableStateOf(false) }
    // Παρατεταμένο OK στο τηλεχειριστήριο: το D-pad δεν παράγει long-click μόνο
    // του, οπότε μετράμε εμείς τη διάρκεια KeyDown -> KeyUp.
    var pressStartMs by remember(channel) { mutableStateOf(0L) }
    // Spring αντί για tween: η κάρτα «κάθεται» με ελαφρύ, φυσικό φρενάρισμα αντί
    // να σταματά απότομα — αυτό δίνει το premium αίσθημα στην εναλλαγή ταινιών.
    // Χωρίς αναπήδηση (NoBouncy), ώστε να μη μοιάζει παιχνιδιάρικο.
    val scale by animateFloatAsState(
        if (focused) motionScale(1.12f) else 1f,
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tvPosterScale"
    )
    // Το λευκό περίγραμμα σβήνει/ανάβει σταδιακά αντί να «πετάγεται».
    val borderAlpha by animateFloatAsState(
        if (focused) 1f else 0f,
        tween(motionDuration(Motion.Focus), easing = Motion.EmphasizedEasing),
        label = "tvPosterBorder"
    )
    // Οι ΜΗ εστιασμένες κάρτες σκουραίνουν: η εστιασμένη «βγαίνει μπροστά» —
    // αυτό είναι που δίνει το premium αίσθημα, περισσότερο κι από το μεγέθυνμα.
    val dim by animateFloatAsState(
        if (focused) 0f else 0.45f,
        tween(motionDuration(Motion.Focus), easing = Motion.EmphasizedEasing),
        label = "tvPosterDim"
    )
    // Σκιά που «σηκώνει» την κάρτα από το φόντο όσο είναι εστιασμένη.
    val elevation by animateDpAsState(
        if (focused) 26.dp else 0.dp,
        tween(motionDuration(Motion.Focus), easing = Motion.EmphasizedEasing),
        label = "tvPosterElevation"
    )
    val shape = RoundedCornerShape(5.dp)
    // ---- ΤΟ ΤΕΛΕΥΤΑΙΟ ΚΟΜΜΑΤΙ ΤΟΥ ΤΡΕΜΟΥΛΟΥ ----
    // Πριν: το ΙΔΙΟ Box ήταν και ο focusable κόμβος και αυτό που έκανε scale.
    // Όταν έπαιρνε focus, τα όριά του ΜΕΓΑΛΩΝΑΝ σταδιακά (250ms animation), και
    // το bring-into-view — που ζητείται από τον focused κόμβο προς ΚΑΘΕ
    // scrollable πρόγονο (LazyRow οριζόντια, LazyColumn κάθετα) — κυνηγούσε
    // κινούμενο στόχο: σκρόλαρε, ξανασκρόλαρε, σε κάθε καρέ.
    //
    // Τώρα: ο focusable κόμβος είναι ΕΞΩΤΕΡΙΚΟ Box με ΣΤΑΘΕΡΑ όρια (116x165 —
    // κρατά από πριν τον χώρο που θα χρειαστεί το scale 1.06). Το scale γίνεται
    // στο ΕΣΩΤΕΡΙΚΟ Box, που είναι απλώς ζωγραφική: δεν αγγίζει ποτέ τα όρια
    // του focus. Ο στόχος του bring-into-view είναι πλέον σταθερός — άρα ένα
    // ομαλό scroll όταν χρειάζεται, και ΜΗΔΕΝ διορθώσεις όταν δεν χρειάζεται.
    Box(
        // Κρατά από πριν τον χώρο του scale 1.12 (109x155 -> 122x174), ώστε τα
        // όρια του focus να μην αλλάζουν ποτέ — αυτό κρατά το layout ακίνητο.
        Modifier
            .width(124.dp)
            .height(176.dp)
            .zIndex(if (focused) 2f else 0f)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused() else pressStartMs = 0L
            }
            .onPreviewKeyEvent { event ->
                val isConfirm = event.key == Key.DirectionCenter ||
                    event.key == Key.Enter ||
                    event.key == Key.NumPadEnter
                if (!isConfirm) return@onPreviewKeyEvent false
                when (event.type) {
                    // Μετράμε τη ΔΙΑΡΚΕΙΑ και αποφασίζουμε στο ΣΗΚΩΜΑ του πλήκτρου.
                    //
                    // Πριν: το μενού άνοιγε ΟΣΟ το OK ήταν ακόμα πατημένο. Μόλις
                    // άνοιγε, το focus έφευγε στον διάλογο — και το KeyUp που
                    // ακολουθούσε παραδιδόταν ΣΤΟΝ ΔΙΑΛΟΓΟ, ενεργοποιώντας το
                    // πρώτο κουμπί («Αναπαραγωγή»). Γι' αυτό η ταινία ξεκινούσε
                    // μόνη της πριν προλάβεις να διαλέξεις.
                    KeyEventType.KeyDown -> {
                        if (pressStartMs == 0L) pressStartMs = android.os.SystemClock.uptimeMillis()
                        true
                    }
                    KeyEventType.KeyUp -> {
                        val held = if (pressStartMs == 0L) 0L
                        else android.os.SystemClock.uptimeMillis() - pressStartMs
                        pressStartMs = 0L
                        if (held >= android.view.ViewConfiguration.getLongPressTimeout().toLong()) {
                            onLongClick()
                        } else {
                            onClick()
                        }
                        true
                    }
                    else -> false
                }
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
    Box(
        Modifier
            .width(109.dp)
            .height(155.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(elevation, shape)
            .clip(shape)
            .background(IptvColors.Surface)
            .border(
                width = 2.5.dp,
                color = Color.White.copy(alpha = borderAlpha),
                shape = shape
            )
    ) {
        if (artwork.isNotBlank()) {
            AsyncImage(
                artwork,
                channel.name,
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Χωρίς artwork: ουδέτερο placeholder + όνομα, ώστε η κάρτα να μην είναι κενή.
            Column(
                Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.Movie,
                    null,
                    tint = IptvColors.TextTertiary,
                    modifier = Modifier.size(30.dp)
                )
                Text(
                    channel.name,
                    color = IptvColors.TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        if (favorite) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.Black.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, "Στη λίστα μου", tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
        // Σκούρο πέπλο στις μη-εστιασμένες κάρτες (πάνω από την αφίσα, κάτω από
        // την μπάρα προόδου): κάνει την εστιασμένη να ξεχωρίζει καθαρά.
        if (dim > 0f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
        }
        // Πρόοδος (Συνέχισε να βλέπεις): λεπτή κόκκινη γραμμή στη βάση της αφίσας.
        progress?.let {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(it.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(IptvColors.Primary)
                )
            }
        }
    }
    }
}
