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
import kotlinx.coroutines.*


@Composable
internal fun SettingsPlaybackDialogs(
    dialogState: MutableState<String>,
    playerModeState: MutableState<String>,
    autoFrameRateModeState: MutableState<String>,
    bufferProfileState: MutableState<String>,
    vm: MainViewModel,
    state: SettingsUiState,
    store: PlaylistStore,
    context: Context
) {
    var dialog by dialogState
    var playerMode by playerModeState
    var autoFrameRateMode by autoFrameRateModeState
    var bufferProfile by bufferProfileState
    val st = state
    val ctx = context
    when (dialog) {
        "font" -> AlertDialog(
            onDismissRequest = { dialog = "" }, containerColor = BgElev2,
            title = { Text("Μέγεθος γραμματοσειράς", color = TextHi) },
            text = {
                // αρχικό focus: χωρίς αυτό, το dialog άνοιγε «νεκρό» σε TV
                val fFont = rememberInitialFocus()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepBtn("A-", Modifier.focusRequester(fFont)) { vm.saveFontScale(st.fontScale - 0.1f) }
                    Text("${(st.fontScale * 100).toInt()}%", color = TextHi,
                        modifier = Modifier.padding(horizontal = 24.dp))
                    StepBtn("A+") { vm.saveFontScale(st.fontScale + 0.1f) }
                }
            },
            confirmButton = {
                TvDialogTextButton(label = "Εντάξει", color = AccentSoft, onClick = { dialog = "" })
            }
        )
        "player" -> AlertDialog(
            onDismissRequest = { dialog = "" }, containerColor = BgElev2,
            title = { Text("Player αναπαραγωγής", color = TextHi) },
            text = {
                Column {
                    val fPl = rememberInitialFocus()   // αλλιώς «νεκρό» dialog σε TV
                    listOf(
                        "auto" to "Αυτόματο (1 → 2 αν χρειαστεί)",
                        "exo" to "Εσωτερικός 1 (ExoPlayer)",
                        "vlc" to "Εσωτερικός 2 (VLC)"
                    ).forEachIndexed { fi, (k, label) ->
                        Row(
                            Modifier.fillMaxWidth()
                                .then(if (fi == 0) Modifier.focusRequester(fPl) else Modifier)
                                .tvFocus(RoundedCornerShape(8.dp)).clickable {
                                    playerMode = k; store.playerMode = k
                                }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = playerMode == k, onClick = {
                                playerMode = k; store.playerMode = k
                            }, colors = RadioButtonDefaults.colors(selectedColor = Accent))
                            Text(label, color = TextHi, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = { TvDialogTextButton(label = "Εντάξει", color = AccentSoft, onClick = { dialog = "" }) }
        )
        "buffer" -> AlertDialog(
            onDismissRequest = { dialog = "" }, containerColor = BgElev2,
            title = { Text("Απόθεμα αναπαραγωγής", color = TextHi) },
            text = {
                Column {
                    val firstFocus = rememberInitialFocus()
                    listOf(
                        Triple(
                            "low", "Χαμηλό",
                            "Γρήγορη αλλαγή καναλιού. Σε ασταθές δίκτυο μπορεί να διακόπτεται."
                        ),
                        Triple(
                            "normal", "Κανονικό",
                            "Ισορροπία ταχύτητας και σταθερότητας. Προτεινόμενο."
                        ),
                        Triple(
                            "high", "Υψηλό",
                            "Αντέχει κακή σύνδεση. Τα κανάλια αργούν λίγο περισσότερο να ανοίξουν."
                        )
                    ).forEachIndexed { index, (key, label, description) ->
                        Row(
                            Modifier.fillMaxWidth()
                                .then(if (index == 0) Modifier.focusRequester(firstFocus) else Modifier)
                                .tvFocus(RoundedCornerShape(8.dp))
                                .clickable {
                                    bufferProfile = key
                                    store.bufferProfile = key
                                }
                                .padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = bufferProfile == key,
                                onClick = {
                                    bufferProfile = key
                                    store.bufferProfile = key
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Accent)
                            )
                            Column(Modifier.padding(start = 4.dp)) {
                                Text(label, color = TextHi, fontSize = 14.sp)
                                Text(description, color = TextMid, fontSize = 11.sp)
                            }
                        }
                    }
                    // Η ρύθμιση κλειδώνεται όταν χτίζεται ο player, οπότε δεν
                    // μπορεί να αλλάξει σε ροή που ήδη παίζει. Καλύτερα να το πει
                    // παρά να φανεί σαν να μην έκανε τίποτα.
                    Text(
                        "Ισχύει από το επόμενο κανάλι ή ταινία που θα ανοίξεις.",
                        color = TextMid,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            },
            confirmButton = {
                TvDialogTextButton(label = "Εντάξει", color = AccentSoft, onClick = { dialog = "" })
            }
        )
        "afr" -> AlertDialog(
            onDismissRequest = { dialog = "" }, containerColor = BgElev2,
            title = { Text("Αυτόματη συχνότητα καρέ", color = TextHi) },
            text = {
                Column {
                    val firstFocus = rememberInitialFocus()
                    listOf(
                        Triple("off", "Απενεργοποιημένο", "Η τηλεόραση μένει στη συχνότητα συστήματος."),
                        Triple("seamless", "Ομαλή αλλαγή", "Αλλάζει μόνο όταν η TV το υποστηρίζει χωρίς μαύρη οθόνη."),
                        Triple("always", "Πλήρης αντιστοίχιση", "Ταιριάζει 24/25/30/50/60 fps· μπορεί να εμφανιστεί στιγμιαία μαύρη οθόνη.")
                    ).forEachIndexed { index, (key, label, description) ->
                        Row(
                            Modifier.fillMaxWidth()
                                .then(if (index == 0) Modifier.focusRequester(firstFocus) else Modifier)
                                .tvFocus(RoundedCornerShape(8.dp))
                                .clickable {
                                    autoFrameRateMode = key
                                    store.autoFrameRateMode = key
                                }
                                .padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = autoFrameRateMode == key,
                                onClick = {
                                    autoFrameRateMode = key
                                    store.autoFrameRateMode = key
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Accent)
                            )
                            Column(Modifier.padding(start = 4.dp)) {
                                Text(label, color = TextHi, fontSize = 14.sp)
                                Text(description, color = TextMid, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TvDialogTextButton(label = "Εντάξει", color = AccentSoft, onClick = { dialog = "" })
            }
        )
        "tmdb" -> {
            var key by remember { mutableStateOf(vm.loadTmdbKey()) }
            AlertDialog(
                onDismissRequest = { dialog = "" }, containerColor = BgElev2,
                title = { Text("TMDB API Key", color = TextHi) },
                text = {
                    Column {
                        val fT = rememberInitialFocus()
                        Text("Δωρεάν key (v3): themoviedb.org → Settings → API. Γεμίζει βαθμολογίες, εικόνες και φωτό ηθοποιών.",
                            color = TextMid, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        SettingField("API Key", key, modifier = Modifier.focusRequester(fT)) { key = it }
                    }
                },
                confirmButton = {
                    TvDialogTextButton(
                        label = "Αποθήκευση",
                        color = AccentSoft,
                        onClick = {
                            vm.saveTmdbKey(key.trim()); toast(ctx, "Αποθηκεύτηκε"); dialog = ""
                        }
                    )
                },
                dismissButton = { TvDialogTextButton(label = "Άκυρο", color = TextMid, onClick = { dialog = "" }) }
            )
        }
        "subs" -> {
            val (k0, u0, p0) = remember { vm.loadSubSettings() }
            var key by remember { mutableStateOf(k0) }
            var user by remember { mutableStateOf(u0) }
            var pass by remember { mutableStateOf(p0) }
            AlertDialog(
                onDismissRequest = { dialog = "" }, containerColor = BgElev2,
                title = { Text("Υπότιτλοι (OpenSubtitles)", color = TextHi) },
                text = {
                    Column {
                        val fS = rememberInitialFocus()
                        Text("Δωρεάν API key: opensubtitles.com → Consumers. Το login δίνει quota για λήψη.",
                            color = TextMid, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        SettingField("API Key", key, modifier = Modifier.focusRequester(fS)) { key = it }
                        SettingField("Username", user) { user = it }
                        SettingField("Password", pass, isPassword = true) { pass = it }
                    }
                },
                confirmButton = {
                    TvDialogTextButton(
                        label = "Αποθήκευση",
                        color = AccentSoft,
                        onClick = {
                            vm.saveSubSettings(key.trim(), user.trim(), pass.trim())
                            toast(ctx, "Αποθηκεύτηκε"); dialog = ""
                        }
                    )
                },
                dismissButton = { TvDialogTextButton(label = "Άκυρο", color = TextMid, onClick = { dialog = "" }) }
            )
        }
    }
}
