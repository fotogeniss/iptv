package com.prelude.iptv.ui.tv.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.R
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration

/**
 * TV Home hero — πιστή απόδοση του Figma (Premium Home Page desktop/TV):
 * kicker, μεγάλος τίτλος 2 γραμμών, IMDb badge + βαθμολογία, γραμμή έτους/είδους,
 * και pills «Αναπαραγωγή» (κόκκινο) + «Πληροφορίες» (ανοιχτό).
 *
 * Όλα τα ύψη είναι ΚΛΕΙΔΩΜΕΝΑ (minLines/σταθερές γραμμές) ώστε καμία ταινία να
 * μην αλλάζει το layout. Τα κείμενα αλλάζουν με Crossfade (καθαρό fade, μηδενική
 * μετακίνηση). Τα κουμπιά είναι ΕΚΤΟΣ του Crossfade — μένουν στατικά.
 */
@Composable
internal fun TvPremiumHomeHero(
    channel: Channel,
    meta: TmdbClient.Meta?,
    // Δίνεται από την οθόνη με βάση το ΠΡΑΓΜΑΤΙΚΟ viewport, ώστε το πρώτο rail
    // (μαζί με το scale της εστιασμένης κάρτας) να χωράει πάντα χωρίς να
    // χρειάζεται κάθετο scroll — αυτό προκαλούσε το τρέμουλο.
    heroHeight: Dp = 318.dp,
    modifier: Modifier = Modifier
) {
    // ΧΩΡΙΣ κουμπιά: το hero είναι καθαρά πληροφοριακό. Οι ενέργειες γίνονται με
    // ΠΑΡΑΤΕΤΑΜΕΝΟ OK πάνω στην κάρτα, ώστε να μη μπερδεύεται το focus ανάμεσα
    // σε κουμπιά και κουτάκια (και να μην «χαλάει» η διάταξη του hero).
    Column(
        modifier
            .fillMaxWidth()
            .height(heroHeight)
            .padding(start = 86.dp, end = 42.dp, top = 40.dp, bottom = 12.dp)
    ) {
        // Πιο αργό fade με emphasized easing: ξεκινά γρήγορα και «κάθεται» απαλά,
        // αντί για γραμμική εναλλαγή. Είναι καθαρό fade (καμία μετακίνηση), οπότε
        // δεν επηρεάζει τη σταθερότητα του layout.
        Crossfade(
            targetState = channel to meta,
            animationSpec = tween(
                durationMillis = motionDuration(Motion.Slow),
                easing = Motion.EmphasizedEasing
            ),
            label = "tvHeroInfo",
            modifier = Modifier.weight(1f)
        ) { (ch, m) ->
            TvHeroInfo(ch, m)
        }
    }
}

/** Σταθερού ύψους (188dp) μπλοκ πληροφοριών — kicker/τίτλος/IMDb/έτος-είδος. */
@Composable
private fun TvHeroInfo(channel: Channel, meta: TmdbClient.Meta?) {
    val title = TmdbClient.cleanTitle(channel.name).ifBlank { channel.name }
    val year = meta?.year?.takeIf(String::isNotBlank) ?: channel.year
    val genre = meta?.genres?.takeIf(String::isNotBlank) ?: channel.genre
    val firstGenre = remember(genre) {
        genre.split(",", "/", "·", "|", "&").map { it.trim() }.firstOrNull { it.isNotBlank() }.orEmpty()
    }
    val rating = meta?.rating?.takeIf(String::isNotBlank)
    val overview = meta?.overview?.takeIf(String::isNotBlank) ?: channel.plot

    // Παίρνει το ύψος που του δίνει το hero (weight) — χωρίς σταθερό ύψος που
    // έκοβε τη γραμμή έτους/είδους σε μικρότερες οθόνες. Οι εσωτερικές γραμμές
    // έχουν σταθερά ύψη, ώστε η εναλλαγή ταινίας να μη μετακινεί τίποτα.
    Column(Modifier.fillMaxHeight()) {
        // Kicker — «PRELUDE SERIES» με το κόκκινο brand γράμμα (Figma: N SERIES).
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("P", color = IptvColors.Primary, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.home_tv_kicker),
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        // Τίτλος — ΠΑΝΤΑ 2 γραμμές (σταθερό ύψος).
        Text(
            title,
            color = Color.White,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Black,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 470.dp)
        )
        Spacer(Modifier.height(8.dp))
        // IMDb badge + βαθμολογία — η γραμμή κρατά ΠΑΝΤΑ το ύψος της.
        Row(Modifier.height(20.dp), verticalAlignment = Alignment.CenterVertically) {
            if (rating != null) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFF5C518))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text("IMDb", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(9.dp))
                Text(stringResource(R.string.home_rating_out_of_ten, rating), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(8.dp))
        // Έτος (κόκκινο) + είδος — σταθερό ύψος γραμμής.
        Row(Modifier.height(20.dp), verticalAlignment = Alignment.CenterVertically) {
            if (year.isNotBlank()) {
                Text(year, color = IptvColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            if (year.isNotBlank() && firstGenre.isNotBlank()) {
                Text("  ·  ", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }
            if (firstGenre.isNotBlank()) {
                Text(firstGenre, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        Spacer(Modifier.height(10.dp))
        // Περίληψη TMDB σε ΔΕΣΜΕΥΜΕΝΟ χώρο 3 γραμμών, με το κείμενο αγκυρωμένο
        // ΚΑΤΩ.
        //
        // Γιατί κάτω: ο χώρος ήταν ήδη κλειδωμένος (γι' αυτό δεν τρέμει τίποτα),
        // αλλά το κείμενο ξεκινούσε από ΠΑΝΩ. Έτσι μια περιγραφή 2 γραμμών άφηνε
        // την 3η κενή από κάτω και το κενό μέχρι τον τίτλο της σειράς φαινόταν
        // μεγαλύτερο — έμοιαζε σαν να «κατέβηκε» η σειρά, ενώ τίποτα δεν είχε
        // μετακινηθεί. Αγκυρωμένο κάτω, η απόσταση από τη σειρά είναι ΣΤΑΘΕΡΗ
        // ανεξάρτητα από το μήκος της περιγραφής.
        Box(
            Modifier.height(51.dp).widthIn(max = 520.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                overview,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Pill κουμπί με ορατό TV focus (λευκό δαχτυλίδι), χωρίς scale — μηδενική κίνηση. */
@Composable
private fun TvHeroPill(
    label: String,
    background: Color,
    foreground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (focused) Color.White else background)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (focused) Color.Black else foreground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}
