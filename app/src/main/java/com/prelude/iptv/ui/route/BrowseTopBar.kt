package com.prelude.iptv.ui.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.LibraryDestination
import com.prelude.iptv.ui.TvIconButton
import com.prelude.iptv.ui.rememberInitialFocus
import com.prelude.iptv.ui.tvFocus

/**
 * Η πάνω μπάρα των ΠΑΛΙΩΝ (κινητό) οθονών: τίτλος πηγής, πλήθος, ταξινόμηση,
 * αναζήτηση και το μενού «Περισσότερα».
 *
 * ΓΙΑΤΙ ΜΟΝΟ ΚΙΝΗΤΟ: στην τηλεόραση κάθε νέα οθόνη (αρχική, ταινίες/σειρές,
 * ζωντανά) έχει δικό της header με το PRELUDE+. Όταν εμφανιζόταν και αυτή,
 * φαινόταν από πάνω το «Xtream …» με το πλήθος στοιχείων — δύο κεφαλίδες.
 *
 * Στο [BrowseRoute] το μπλοκ αυτό ζούσε μέσα σε `if (!isTv)`, όμως κρατούσε
 * ακόμη κλαδιά `if (isTv)` από την εποχή που η μπάρα ήταν κοινή: ένα focus
 * requester που δεν ενεργοποιούνταν ποτέ και μια αναζήτηση με δύο δρόμους από
 * τους οποίους ο ένας ήταν άφταστος. Εδώ λείπουν — η μπάρα είναι εξ ορισμού
 * κινητού και δεν χρειάζεται να ρωτά κάθε φορά πού τρέχει.
 *
 * Χωρίς κατάσταση, εκτός από το άνοιγμα των δύο μενού της: αυτά αφορούν μόνο
 * τη μπάρα και δεν έχει λόγο να τα ξέρει ο καλών.
 */
@Composable
internal fun BrowseLegacyTopBar(
    /** Όνομα της τρέχουσας λίστας/πηγής. */
    playlistName: String,
    /** Πόσα στοιχεία φαίνονται τώρα (μετά από group/αναζήτηση). */
    visibleCount: Int,
    /** Πόσα στοιχεία έχει συνολικά η πηγή· 0 = δεν έχει φορτώσει ακόμη. */
    totalCount: Int,
    sortMode: String,
    searchOpen: Boolean,
    searchText: String,
    /** Δείχνει το κουμπί «Πρόγραμμα (Grid)» — μόνο σε ζωντανά με φορτωμένο EPG. */
    showEpgGrid: Boolean,
    startInset: Dp,
    onBack: () -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSortMode: (String) -> Unit,
    onOpenEpgGrid: () -> Unit,
    onChooseContent: () -> Unit,
    onChangeCategories: () -> Unit,
    onRefresh: () -> Unit,
    onOpenLibrary: (LibraryDestination) -> Unit,
    onExport: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(Bg)
            .padding(start = startInset)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (searchOpen) {
            TvIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Κλείσιμο", onClick = onSearchClose)
            InlineSearchField(
                value = searchText,
                onChange = onSearchChange,
                onClear = { onSearchChange("") },
                modifier = Modifier.weight(1f)
            )
            return@Row
        }

        TvIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω", onClick = onBack)
        Column(Modifier.weight(1f).padding(end = 4.dp)) {
            Text(
                playlistName, color = TextHi, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (totalCount > 0) {
                // «132 από 4.500» όταν φιλτράρεις — όχι το σύνολο σκέτο, που
                // έδειχνε λάθος νούμερο σε group/αναζήτηση.
                Text(
                    if (visibleCount != totalCount) "$visibleCount από $totalCount"
                    else "$totalCount στοιχεία",
                    color = TextLo, fontSize = 11.sp, maxLines = 1
                )
            }
        }

        // Ταξινόμηση: σειρά παρόχου / Α-Ω / έτος — δίπλα στην αναζήτηση
        var sortOpen by remember { mutableStateOf(false) }
        Box {
            TvIconButton(Icons.Default.SwapVert, "Ταξινόμηση") { sortOpen = true }
            DropdownMenu(
                expanded = sortOpen, onDismissRequest = { sortOpen = false },
                containerColor = BgElev2
            ) {
                val mf = rememberInitialFocus()
                SORT_MODES.forEachIndexed { i, (key, label) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                label,
                                color = if (sortMode == key) AccentSoft else TextHi,
                                fontWeight = if (sortMode == key) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = (if (i == 0) Modifier.focusRequester(mf) else Modifier)
                            .tvFocus(RoundedCornerShape(6.dp)),
                        onClick = { onSortMode(key); sortOpen = false }
                    )
                }
            }
        }

        if (showEpgGrid)
            TvIconButton(Icons.Default.CalendarMonth, "Πρόγραμμα (Grid)", onClick = onOpenEpgGrid)
        TvIconButton(Icons.Default.Search, "Αναζήτηση", onClick = onSearchOpen)

        var menuOpen by remember { mutableStateOf(false) }
        Box {
            TvIconButton(Icons.Default.MoreVert, "Περισσότερα", tint = TextMid) { menuOpen = true }
            DropdownMenu(
                expanded = menuOpen, onDismissRequest = { menuOpen = false },
                containerColor = BgElev2
            ) {
                val first = rememberInitialFocus()
                MoreItem(
                    "Ενότητες (Live / Ταινίες / Σειρές)", Icons.Default.Category, AccentSoft,
                    Modifier.focusRequester(first)
                ) { menuOpen = false; onChooseContent() }
                MoreItem("Κατηγορίες / Ομάδες…", Icons.Default.Tune, AccentSoft) {
                    menuOpen = false; onChangeCategories()
                }
                MoreItem("Ανανέωση", Icons.Default.Refresh, TextMid) {
                    menuOpen = false; onRefresh()
                }
                MoreItem("Η λίστα μου", Icons.Default.Bookmark, TextMid) {
                    menuOpen = false; onOpenLibrary(LibraryDestination.MY_LIST)
                }
                MoreItem("Συνέχισε να βλέπεις", Icons.Default.PlayCircle, TextMid) {
                    menuOpen = false; onOpenLibrary(LibraryDestination.CONTINUE_WATCHING)
                }
                MoreItem("Ιστορικό", Icons.Default.History, TextMid) {
                    menuOpen = false; onOpenLibrary(LibraryDestination.HISTORY)
                }
                MoreItem("Εξαγωγή", Icons.Default.IosShare, TextMid) {
                    menuOpen = false; onExport()
                }
            }
        }
    }
}

/** key -> ετικέτα. Η σειρά είναι και η σειρά εμφάνισης. */
private val SORT_MODES = listOf(
    "default" to "Σειρά παρόχου",
    "az" to "Α → Ω",
    "za" to "Ω → Α",
    "year" to "Έτος (νεότερα πρώτα)",
)

/**
 * Μία γραμμή του «Περισσότερα». Επτά πανομοιότυπα [DropdownMenuItem] με το ίδιο
 * σχήμα focus και το ίδιο χρώμα κειμένου είναι επτά ευκαιρίες να ξεχαστεί το ένα.
 */
@Composable
private fun MoreItem(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label, color = TextHi) },
        leadingIcon = { Icon(icon, null, tint = tint) },
        modifier = modifier.tvFocus(RoundedCornerShape(6.dp)),
        onClick = onClick
    )
}
