@file:android.annotation.SuppressLint("ProduceStateDoesNotAssignValue")

package com.prelude.iptv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.TvDialogTextButton
import kotlinx.coroutines.CancellationException

/** Μία εγγραφή προγράμματος, ανεξάρτητη από την πηγή που τη γέννησε. */
data class PlayerProgramme(
    val time: String,
    val title: String,
    val description: String,
    val isNow: Boolean,
)

/**
 * Το πρόγραμμα ενός καναλιού σε μορφή έτοιμη για προβολή.
 *
 * ΜΙΑ υλοποίηση: η μορφοποίηση της ώρας και —κυρίως— ο υπολογισμός του «τώρα»
 * ήταν γραμμένα ξεχωριστά για τη λίστα ζωντανών. Δύο αντίγραφα του ίδιου
 * υπολογισμού σημαίνει ότι κάποια στιγμή θα δείχνουν διαφορετικό πρόγραμμα για
 * το ίδιο κανάλι — έχει ήδη συμβεί μία φορά σε αυτή την εφαρμογή.
 */
fun upcomingProgrammes(tvgId: String, hours: Int = 24): List<PlayerProgramme> = runCatching {
    val nowMs = System.currentTimeMillis()
    val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    com.prelude.iptv.data.EpgManager.upcoming(tvgId, hours).map { programme ->
        PlayerProgramme(
            time = format.format(java.util.Date(programme.startMs)),
            title = programme.title,
            description = programme.desc,
            isNow = nowMs in programme.startMs until programme.stopMs,
        )
    }
}.getOrDefault(emptyList())

/**
 * Το πρόγραμμα του καναλιού που παίζει, χωρίς έξοδο από τον player.
 *
 * Ο παλιός player είχε πίνακα EPG· ο νέος τον έχασε στη μετάβαση, και στα
 * ζωντανά από την αρχική δεν υπήρχε κανένας τρόπος να δεις τι παίζει μετά.
 *
 * Η λήψη γίνεται ΟΤΑΝ ανοίξει ο διάλογος και όχι νωρίτερα: το EPG ενός αργού
 * portal κάνει δευτερόλεπτα, και δεν υπάρχει λόγος να το πληρώνει κάθε άνοιγμα
 * καναλιού για μια πληροφορία που οι περισσότεροι δεν ζητούν.
 */
@Composable
fun PlayerEpgDialog(
    channelName: String,
    load: suspend () -> List<PlayerProgramme>,
    onDismiss: () -> Unit,
) {
    val programmes by produceState<List<PlayerProgramme>?>(initialValue = null, channelName) {
        value = try {
            load()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IptvColors.SurfaceRaised,
        title = {
            Text(
                channelName,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            val loadedProgrammes = programmes
            when {
                loadedProgrammes == null -> Text(
                    "Φόρτωση προγράμματος…",
                    color = IptvColors.TextSecondary,
                    fontSize = 13.sp
                )
                loadedProgrammes.isEmpty() -> Text(
                    "Δεν υπάρχει διαθέσιμο πρόγραμμα για αυτό το κανάλι.",
                    color = IptvColors.TextSecondary,
                    fontSize = 13.sp
                )
                else -> LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(loadedProgrammes) { programme ->
                        Row(Modifier.fillMaxWidth()) {
                            // Η ώρα σε σταθερό πλάτος: αλλιώς οι τίτλοι
                            // ξεκινούσαν σε διαφορετικό σημείο ανά γραμμή και η
                            // στήλη διαβαζόταν σαν σκάλα.
                            Text(
                                programme.time,
                                color = if (programme.isNow) IptvColors.Primary
                                else IptvColors.TextTertiary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.width(56.dp)
                            )
                            Column(Modifier.fillMaxWidth()) {
                                Text(
                                    programme.title,
                                    color = if (programme.isNow) Color.White
                                    else IptvColors.TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (programme.isNow) FontWeight.Black
                                    else FontWeight.SemiBold
                                )
                                if (programme.isNow && programme.description.isNotBlank()) {
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        programme.description,
                                        color = IptvColors.TextTertiary,
                                        fontSize = 11.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (programme.isNow) {
                            Spacer(Modifier.height(2.dp))
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(IptvColors.Primary.copy(alpha = 0.35f))
                                    .padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TvDialogTextButton(label = "Κλείσιμο", color = Color.White, onClick = onDismiss)
        }
    )
}
