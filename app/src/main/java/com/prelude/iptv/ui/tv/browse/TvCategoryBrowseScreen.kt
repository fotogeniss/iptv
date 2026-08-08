@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.prelude.iptv.ui.tv.browse

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed as itemsIndexedColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.UiState
import com.prelude.iptv.ui.greekUppercase
import com.prelude.iptv.ui.components.rememberPosterArtwork
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale
import com.prelude.iptv.ui.requestFocusWithRetry
import com.prelude.iptv.ui.tvConfirm
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Περιήγηση ενότητας για τηλεόραση: κατηγορίες αριστερά, πλέγμα αφισών δεξιά.
 *
 * Χρησιμοποιείται σε Ταινίες / Σειρές / Ζωντανά — ΟΧΙ στην Αρχική, που κρατά το
 * cinematic hero. Έτσι, μπαίνοντας σε ενότητα, βλέπεις μια καθαρή «βιβλιοθήκη»
 * αντί για επανάληψη της αρχικής.
 *
 * Φόντο: καθαρό μαύρο. Χρώματα: το κόκκινο brand της εφαρμογής.
 */
@Composable
fun TvCategoryBrowseScreen(
    groups: List<String>,
    selectedGroup: String,
    channels: List<Channel>,
    favoriteKeys: Set<String>,
    contentType: String,
    onSelectGroup: (String) -> Unit,
    onOpen: (Channel) -> Unit,
    onLongOpen: (Channel) -> Unit = {},
    /**
     * Το κλειδί του στοιχείου που άνοιξε τελευταίο, ή null.
     *
     * Επιστρέφοντας από την αναπαραγωγή, το focus πρέπει να προσγειώνεται ΕΚΕΙ —
     * όχι στην κορυφή και όχι στο αριστερό μενού. Χάνεις τη θέση σου σε πλέγμα
     * χιλιάδων ταινιών και πρέπει να την ξαναβρείς με το χέρι.
     */
    lastOpenedKey: String? = null,
    /** true όσο παίζει κάτι από πάνω· στο false επιστρέφει το focus. */
    obscuredByPlayer: Boolean = false,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val groupsState = rememberLazyListState()
    val firstGroupFocus = remember { FocusRequester() }
    val lastOpenedFocus = remember { FocusRequester() }
    var initialFocusDone by remember { mutableStateOf(false) }

    // Αλλάζοντας κατηγορία, το πλέγμα ξεκινά από την κορυφή.
    LaunchedEffect(selectedGroup, contentType) { gridState.scrollToItem(0) }
    LaunchedEffect(Unit) {
        if (!initialFocusDone) {
            firstGroupFocus.requestFocusWithRetry()
            initialFocusDone = true
        }
    }

    // ΕΠΙΣΤΡΟΦΗ ΑΠΟ ΤΗΝ ΑΝΑΠΑΡΑΓΩΓΗ: πίσω στο στοιχείο που έπαιζε.
    //
    // Το πλέγμα είναι lazy — το στοιχείο μπορεί να έχει αποσυντεθεί όσο έπαιζε ο
    // player. Γι' αυτό πρώτα κάνουμε scroll ώστε να ξαναϋπάρχει, και μόνο μετά
    // ζητάμε focus. Χωρίς το scroll, το αίτημα θα πήγαινε σε ανύπαρκτο κόμβο.
    var wasObscured by remember { mutableStateOf(false) }
    LaunchedEffect(obscuredByPlayer, lastOpenedKey) {
        if (obscuredByPlayer) {
            wasObscured = true
            return@LaunchedEffect
        }
        if (!wasObscured) return@LaunchedEffect
        wasObscured = false
        val key = lastOpenedKey ?: return@LaunchedEffect
        val index = channels.indexOfFirst { PlaybackQueue.favKey(it) == key }
        if (index < 0) return@LaunchedEffect
        gridState.scrollToItem(index)
        lastOpenedFocus.requestFocusWithRetry()
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        // Το περιθώριο για τη μπάρα εικονιδίων μπαίνει ΕΔΩ, στο περιεχόμενο, ώστε
        // το μαύρο φόντο να πιάνει ολόκληρη την οθόνη.
        Column(Modifier.fillMaxSize().padding(start = 64.dp)) {
            TvBrowseHeader()
            Row(Modifier.fillMaxSize()) {
                // ---------- Αριστερά: κατηγορίες ----------
                LazyColumn(
                    modifier = Modifier.width(268.dp).fillMaxHeight(),
                    state = groupsState,
                    contentPadding = PaddingValues(start = 30.dp, end = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexedColumn(groups, key = { index, g -> "tv-group:$index:$g" }) { index, group ->
                        TvGroupRow(
                            // «Όλα τα κανάλια» έχει νόημα μόνο στα ζωντανά. Στις
                            // ταινίες/σειρές λέμε αυτό που πραγματικά δείχνει.
                            label = allGroupLabel(group, contentType),
                            selected = group == selectedGroup,
                            modifier = if (index == 0) Modifier.focusRequester(firstGroupFocus) else Modifier,
                            onClick = { onSelectGroup(group) }
                        )
                    }
                }

                // ---------- Δεξιά: πλέγμα αφισών ----------
                if (channels.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Δεν υπάρχει περιεχόμενο σε αυτή την κατηγορία",
                            color = IptvColors.TextSecondary,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 8.dp, end = 34.dp, bottom = 34.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        itemsIndexed(
                            channels,
                            key = { index, ch -> "tv-grid:$index:${PlaybackQueue.favKey(ch)}" }
                        ) { _, channel ->
                            val key = PlaybackQueue.favKey(channel)
                            TvPosterTile(
                                channel = channel,
                                favorite = key in favoriteKeys,
                                // Ο requester κρεμιέται ΜΟΝΟ στο στοιχείο που
                                // άνοιξε τελευταίο — εκεί επιστρέφει το focus.
                                modifier = if (lastOpenedKey != null && key == lastOpenedKey) {
                                    Modifier.focusRequester(lastOpenedFocus)
                                } else Modifier,
                                onClick = { onOpen(channel) },
                                onLongClick = { onLongOpen(channel) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Η ετικέτα της κατηγορίας «όλα», προσαρμοσμένη στην ενότητα.
 *
 * Η σταθερά είναι «Όλα τα κανάλια» (φτιάχτηκε για τα ζωντανά) και φαινόταν
 * λάθος στις ταινίες και τις σειρές. Αλλάζουμε μόνο την ΕΜΦΑΝΙΣΗ — η τιμή που
 * ταξιδεύει στο state μένει η ίδια, ώστε να μη σπάσει το φιλτράρισμα.
 */
internal fun allGroupLabel(group: String, contentType: String): String =
    if (group != UiState.ALL_GROUP) group
    else when (contentType) {
        "vod" -> "Όλες οι ταινίες"
        "series" -> "Όλες οι σειρές"
        else -> group
    }

/** Κοινή κεφαλίδα ενοτήτων (χρησιμοποιείται και από την οθόνη Live). */
@Composable
internal fun TvBrowseHeaderPublic() = TvBrowseHeader()

@Composable
private fun TvBrowseHeader() {
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            nowMs = System.currentTimeMillis()
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(start = 30.dp, end = 34.dp, top = 18.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("PRELUDE", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Text("+", color = IptvColors.Primary, fontSize = 21.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(14.dp))
        Text(
            SimpleDateFormat("EEE d, HH:mm", Locale.getDefault()).format(Date(nowMs)),
            color = IptvColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Γραμμή κατηγορίας: κόκκινη όταν είναι επιλεγμένη, λευκή όταν έχει focus. */
@Composable
private fun TvGroupRow(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    val background = when {
        focused -> Color.White
        selected -> IptvColors.Primary
        else -> Color.Transparent
    }
    val foreground = when {
        focused -> Color.Black
        selected -> Color.White
        else -> IptvColors.TextSecondary
    }
    Text(
        // greekUppercase: τα ελληνικά κεφαλαία δεν παίρνουν τόνους.
        label.greekUppercase(),
        color = foreground,
        fontSize = 11.sp,
        letterSpacing = 0.6.sp,
        fontWeight = if (selected || focused) FontWeight.Black else FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background, shape)
            .onFocusChanged { focused = it.isFocused }
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp)
    )
}

/** Κάθετη αφίσα με τίτλο από κάτω — όπως στο πλέγμα των εικόνων. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TvPosterTile(
    channel: Channel,
    favorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val artwork = rememberPosterArtwork(channel)
    var focused by remember { mutableStateOf(false) }
    var pressStartMs by remember(channel) { mutableStateOf(0L) }
    val scale by animateFloatAsState(
        if (focused) motionScale(1.07f) else 1f,
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tvTileScale"
    )
    val dim by animateFloatAsState(
        if (focused) 0f else 0.35f,
        tween(motionDuration(Motion.Focus), easing = Motion.EmphasizedEasing),
        label = "tvTileDim"
    )
    val elevation by animateDpAsState(
        if (focused) 22.dp else 0.dp,
        tween(motionDuration(Motion.Focus), easing = Motion.EmphasizedEasing),
        label = "tvTileElevation"
    )
    val shape = RoundedCornerShape(6.dp)

    Column(
        modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            // Το παρατεταμένο OK του τηλεχειριστηρίου δεν περνά από το
            // combinedClickable — χρειάζεται ρητός χρονισμός πλήκτρου.
            .tvConfirm(onClick = onClick, onLongClick = onLongClick)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(elevation, shape)
                .clip(shape)
                .background(IptvColors.Surface)
                .border(
                    width = 2.5.dp,
                    color = Color.White.copy(alpha = if (focused) 1f else 0f),
                    shape = shape
                )
        ) {
            if (artwork.isNotBlank()) {
                AsyncImage(artwork, channel.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(
                    Icons.Outlined.Movie, null,
                    tint = IptvColors.TextTertiary,
                    modifier = Modifier.align(Alignment.Center).size(34.dp)
                )
            }
            if (favorite) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp)
                        .clip(RoundedCornerShape(99.dp)).background(Color.Black.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, "Στη λίστα μου", tint = Color.White, modifier = Modifier.size(13.dp))
                }
            }
            if (dim > 0f) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            channel.name,
            color = if (focused) Color.White else IptvColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
