package com.prelude.iptv.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

private val NavRed = IptvColors.Primary
private val NavMuted = Color(0xFF8E8E96)

/**
 * TV icon rail — πιστό στο Figma «Premium Home Page desktop/TV»: στενή στήλη
 * ΜΟΝΟ με εικονίδια, χωρίς labels/expansion. Το ενεργό προορισμό τον δείχνει
 * λευκό εικονίδιο + κόκκινη υπογράμμιση· το focus, λευκός κύκλος 10%.
 * Το signature μένει ίδιο με πριν ώστε να μην αλλάξει κανένας caller.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PremiumTvNavigationRail(
    profileName: String,
    currentContentType: String,
    homeSelected: Boolean,
    libraryDestination: LibraryDestination?,
    epgAvailable: Boolean,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onMyList: () -> Unit,
    onContinueWatching: () -> Unit,
    onHistory: () -> Unit,
    onLive: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onEpg: () -> Unit,
    onSources: () -> Unit,
    onSettings: () -> Unit,
    // true όταν το focus είναι στο μενού: ανοίγει και δείχνει τα ονόματα.
    expanded: Boolean = false,
    /**
     * Προσαρτάται στο ΕΝΕΡΓΟ στοιχείο, ώστε το BACK να προσγειώνεται εκεί που
     * βρίσκεται ο χρήστης και όχι στην κορυφή της λίστας.
     */
    selectedFocus: androidx.compose.ui.focus.FocusRequester? = null,
    /**
     * false = κανένα στοιχείο του μενού δεν είναι εστιάσιμο.
     *
     * Το μενού παραμένει ΟΡΑΤΟ (κλειστό, με τα εικονίδια) αλλά αόρατο για το
     * σύστημα focus. Έτσι δεν μπορεί να αρπάξει το αρχικό focus όταν οι λίστες
     * περιεχομένου δεν έχουν προλάβει να συντεθούν — που ήταν η αιτία που άνοιγε
     * μόνο του στην εκκίνηση.
     */
    interactive: Boolean = true,
    modifier: Modifier = Modifier
) {
    // 178dp ανοιχτό: χωράει «Η λίστα μου» και «Οδηγός TV» χωρίς κόψιμο, και
    // σταματά ΠΡΙΝ τη στήλη κατηγοριών. Στα 232dp σκέπαζε τις κατηγορίες, οπότε
    // όσο περιηγόσουν στο μενού δεν έβλεπες πού βρισκόσουν.
    val railWidth by androidx.compose.animation.core.animateDpAsState(
        if (expanded) 178.dp else 64.dp,
        androidx.compose.animation.core.tween(220),
        label = "navRailWidth"
    )
    Column(
        modifier
            .width(railWidth)
            .fillMaxHeight()
            .background(
                // ΑΝΟΙΧΤΗ: συμπαγές φόντο. Με βαθμίδα που κατέληγε διάφανη, το
                // γκρι highlight του στοιχείου (που πιάνει όλο το πλάτος)
                // «ξεχυνόταν» πάνω στα κανάλια.
                // ΚΛΕΙΣΤΗ: βαθμίδα, ώστε τα εικονίδια να πατούν διακριτικά πάνω
                // στο cinematic backdrop.
                if (expanded) {
                    androidx.compose.ui.graphics.SolidColor(Color(0xF20A0A0C))
                } else {
                    Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.92f), Color.Black.copy(alpha = 0.60f), Color.Transparent)
                    )
                }
            )
            .focusGroup()
            // ΣΤΑΘΕΡΗ ΓΕΩΜΕΤΡΙΑ: ίδιο περιθώριο και ίδια στοίχιση σε κάθε
            // κατάσταση. Μόνο το ΠΛΑΤΟΣ του πλαισίου αλλάζει — τα εικονίδια
            // μένουν καρφωμένα στη θέση τους και «ξεδιπλώνονται» μόνο τα ονόματα
            // δεξιά τους. Πριν άλλαζαν ταυτόχρονα στοίχιση, πλάτος και περιθώρια,
            // και τα εικονίδια γλιστρούσαν στη νέα τους θέση.
            .padding(vertical = 18.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Brand — μικρό κόκκινο "P" όπως το N του Figma. Σε κουτί 40dp, ίδιο με
        // τα εικονίδια, ώστε να είναι στοιχισμένο μαζί τους και να μη μετακινείται.
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)).background(NavRed),
                contentAlignment = Alignment.Center
            ) {
                Text("P", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(14.dp))

        // ---- ΠΟΙΟ ΣΤΟΙΧΕΙΟ ΕΙΝΑΙ ΕΝΕΡΓΟ ----
        //
        // Υπολογίζονται μία φορά, γιατί χρειάζονται ΔΥΟ φορές: για το χρώμα, και
        // για να ξέρουμε πού πρέπει να προσγειωθεί το focus όταν έρθει το BACK.
        val searchSelected = libraryDestination == LibraryDestination.SEARCH
        val myListSelected = libraryDestination == LibraryDestination.MY_LIST
        val continueSelected = libraryDestination == LibraryDestination.CONTINUE_WATCHING
        val historySelected = libraryDestination == LibraryDestination.HISTORY
        val liveSelected = libraryDestination == null && currentContentType == "live" && !homeSelected
        val moviesSelected = libraryDestination == null && currentContentType == "vod" && !homeSelected
        val seriesSelected = libraryDestination == null && currentContentType == "series" && !homeSelected
        val homeIsSelected = homeSelected && libraryDestination == null

        // Το BACK πρέπει να προσγειώνεται εκεί που ΕΙΣΑΙ, όχι στην κορυφή.
        //
        // Ο FocusRequester ήταν δεμένος στο focusGroup που τα περιέχει όλα, και
        // ένα focusGroup δίνει το focus στο ΠΡΩΤΟ του παιδί. Έτσι, από τις Σειρές
        // κατέληγες στην «Αναζήτηση» και έπρεπε να κατέβεις έξι θέσεις για να δεις
        // πού βρισκόσουν.
        //
        // Αν δεν είναι τίποτα ενεργό (π.χ. Οδηγός TV), πέφτει στην Αρχική: πάντα
        // υπάρχει προορισμός, ποτέ αίτημα που αποτυγχάνει σιωπηλά.
        val noneSelected = !searchSelected && !myListSelected && !continueSelected &&
            !historySelected && !liveSelected && !moviesSelected && !seriesSelected &&
            !homeIsSelected
        // Απλή επιλογή, όχι Composable: δεν διαβάζει κατάσταση, μόνο αποφασίζει
        // σε ποιο στοιχείο κρεμιέται ο ένας FocusRequester.
        val focusFor: (Boolean) -> androidx.compose.ui.focus.FocusRequester? =
            { isSelected -> selectedFocus?.takeIf { isSelected } }

        TvNavIcon(Icons.Default.Search, "Αναζήτηση", searchSelected, onSearch, expanded, focusFor(searchSelected), interactive)
        TvNavIcon(Icons.Default.Home, "Αρχική", homeIsSelected, onHome, expanded, focusFor(homeIsSelected || noneSelected), interactive)
        TvNavIcon(Icons.Default.Bookmark, "Η λίστα μου", myListSelected, onMyList, expanded, focusFor(myListSelected), interactive)
        TvNavIcon(Icons.Default.PlayCircle, "Συνέχισε", continueSelected, onContinueWatching, expanded, focusFor(continueSelected), interactive)
        TvNavIcon(Icons.Default.History, "Ιστορικό", historySelected, onHistory, expanded, focusFor(historySelected), interactive)
        TvNavIcon(Icons.Default.LiveTv, "Ζωντανά", liveSelected, onLive, expanded, focusFor(liveSelected), interactive)
        TvNavIcon(Icons.Default.Movie, "Ταινίες", moviesSelected, onMovies, expanded, focusFor(moviesSelected), interactive)
        TvNavIcon(Icons.Default.Tv, "Σειρές", seriesSelected, onSeries, expanded, focusFor(seriesSelected), interactive)
        // Πάντα κλικαρίσιμος — χωρίς EPG δείχνει ενημερωτικό μήνυμα.
        TvNavIcon(Icons.Default.CalendarMonth, "Οδηγός TV", false, onEpg, expanded, null, interactive)

        Spacer(Modifier.weight(1f))
        TvNavIcon(Icons.Default.VideoLibrary, "Πηγές", false, onSources, expanded, null, interactive)
        TvNavIcon(Icons.Default.Settings, "Ρυθμίσεις", false, onSettings, expanded, null, interactive)
        Spacer(Modifier.height(8.dp))
        // Προφίλ (μη-focusable ένδειξη).
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF34343A)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                profileName.trim().firstOrNull()?.uppercase() ?: "P",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun TvNavIcon(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    expanded: Boolean = false,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    /**
     * false = το στοιχείο δεν είναι ΚΑΝ εστιάσιμο.
     *
     * Το clickable είναι αυτό που κάνει ένα στοιχείο εστιάσιμο· με enabled=false
     * παύει να υπάρχει για το σύστημα focus. Είναι ρητό και ντετερμινιστικό, σε
     * αντίθεση με το focusProperties πάνω σε focusGroup — εκείνο δηλώνει ότι δεν
     * είναι εστιάσιμη η ΟΜΑΔΑ, αφήνοντας τα παιδιά της προσβάσιμα. Γι' αυτό το
     * μενού συνέχιζε να αρπάζει το focus στην εκκίνηση.
     */
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(horizontalAlignment = Alignment.Start) {
        // ΤΟ ΕΙΚΟΝΙΔΙΟ ΔΕΝ ΜΕΤΑΚΙΝΕΙΤΑΙ ΠΟΤΕ.
        //
        // Πριν, ανοίγοντας το μενού άλλαζαν ταυτόχρονα η στοίχιση (κέντρο ->
        // αριστερά), το πλάτος και τα περιθώρια: τα εικονίδια «γλιστρούσαν» στη
        // νέα τους θέση και η κίνηση φαινόταν πρόχειρη.
        //
        // Τώρα η γεωμετρία του εικονιδίου είναι σταθερή σε κάθε κατάσταση — 40dp
        // κουτί, στοιχισμένο αριστερά, πάντα στο ίδιο σημείο. Αλλάζει ΜΟΝΟ το αν
        // εμφανίζεται το κείμενο δίπλα του.
        Row(
            Modifier
                // ΠΡΙΝ το clickable: ο FocusRequester πρέπει να προηγείται του
                // κόμβου που κάνει το στοιχείο εστιάσιμο, αλλιώς δεν τον βρίσκει.
                .then(
                    if (focusRequester != null) {
                        Modifier.focusRequester(focusRequester)
                    } else Modifier
                )
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(if (expanded) 9.dp else 20.dp))
                .background(if (focused) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription,
                    tint = if (focused || selected) Color.White else NavMuted,
                    modifier = Modifier.size(21.dp)
                )
            }
            // Το όνομα εμφανίζεται ΜΟΝΟ όταν είσαι στο μενού: αλλιώς έπρεπε να
            // μαντεύεις τι σημαίνει κάθε εικονίδιο.
            if (expanded) {
                Spacer(Modifier.width(6.dp))
                Text(
                    contentDescription,
                    color = if (focused || selected) Color.White else NavMuted,
                    fontSize = 13.sp,
                    fontWeight = if (selected || focused) FontWeight.Black else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // Κόκκινη υπογράμμιση ενεργού — όπως στο Figma. Πάντα δεσμευμένο ύψος
        // ώστε το επιλεγμένο/μη να μην αλλάζει το κατακόρυφο layout.
        Box(
            Modifier
                .padding(top = 2.dp)
                .width(20.dp)
                .height(2.5.dp)
                .background(if (selected) NavRed else Color.Transparent, RoundedCornerShape(99.dp))
        )
    }
}
