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
internal fun SettingsAccountDialogs(
    dialogState: MutableState<String>,
    vm: MainViewModel,
    context: Context,
    onExportBackup: (String) -> Unit,
    onImportBackup: (String) -> Unit
) {
    var dialog by dialogState
    val ctx = context
    when (dialog) {
        "profiles" -> {
            var adding by remember { mutableStateOf(false) }
            var newName by remember { mutableStateOf("") }
            var newProtected by remember { mutableStateOf(false) }
            var askPinFor by remember { mutableStateOf<Int?>(null) }
            val profiles by vm.profilesState.collectAsStateWithLifecycle()
            val active = vm.activeProfileId()

            val switchTo: (Int) -> Unit = { id ->
                vm.setActiveProfile(id)
                toast(ctx, "Αλλαγή προφίλ — επανεκκίνηση…")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                        ?.putExtra(AppRouteContract.EXTRA_SKIP_PROFILE_GATE, true)
                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    if (launch != null) ctx.startActivity(launch)
                }, 350)
            }

            askPinFor?.let { id ->
                PinDialog(
                    title = "PIN για αλλαγή προφίλ",
                    onOk = { pin ->
                        if (vm.checkPin(pin)) { askPinFor = null; dialog = ""; switchTo(id) }
                        else toast(ctx, "Λάθος PIN")
                    },
                    onCancel = { askPinFor = null }
                )
            }

            AlertDialog(
                onDismissRequest = { dialog = "" }, containerColor = BgElev2,
                title = { Text(if (adding) "Νέο προφίλ" else "Προφίλ", color = TextHi) },
                text = {
                    Column {
                        if (adding) {
                            val fN = rememberInitialFocus()
                            SettingField("Όνομα", newName, modifier = Modifier.focusRequester(fN)) { newName = it }
                            Row(
                                Modifier.fillMaxWidth().tvFocus(RoundedCornerShape(8.dp))
                                    .clickable { newProtected = !newProtected }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = newProtected, onCheckedChange = { newProtected = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Accent))
                                Column {
                                    Text("Προστασία με PIN", color = TextHi, fontSize = 13.sp)
                                    Text("Θα ζητά το PIN για είσοδο σε αυτό το προφίλ",
                                        color = TextLo, fontSize = 11.sp)
                                }
                            }
                        } else {
                            Text(
                                "Κάθε προφίλ έχει δικά του αγαπημένα, «συνέχισε να βλέπεις» και κλειδώματα. Οι λίστες είναι κοινές.",
                                color = TextMid, fontSize = 12.sp, lineHeight = 17.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            val fP = rememberInitialFocus()
                            profiles.forEachIndexed { i, p ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .then(if (i == 0) Modifier.focusRequester(fP) else Modifier)
                                        .tvFocus(RoundedCornerShape(8.dp))
                                        .clickable(enabled = p.id != active) {
                                            if (vm.profileNeedsPin(p)) askPinFor = p.id
                                            else { dialog = ""; switchTo(p.id) }
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (p.id == active) Icons.Default.CheckCircle else Icons.Default.AccountCircle,
                                        null, tint = if (p.id == active) Accent else TextMid,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        (if (p.protected) "🔒 " else "") + p.name,
                                        color = if (p.id == active) TextHi else TextMid,
                                        fontWeight = if (p.id == active) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (p.id != 0) IconButton(onClick = {
                                        vm.deleteProfile(p.id); toast(ctx, "Διαγράφηκε: ${p.name}")
                                        dialog = ""
                                    }, modifier = Modifier.size(28.dp).tvFocus(RoundedCornerShape(6.dp))) {
                                        Icon(Icons.Default.Delete, "Διαγραφή", tint = TextLo,
                                            modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (adding) TextButton(
                        enabled = newName.isNotBlank(),
                        onClick = {
                            if (newProtected && !vm.hasParentalPin())
                                toast(ctx, "Όρισε πρώτα PIN στον Γονικό έλεγχο")
                            else { vm.addProfile(newName, newProtected); adding = false; newName = ""; newProtected = false }
                        },
                        modifier = Modifier.tvFocus(RoundedCornerShape(8.dp))
                    ) { Text("Προσθήκη", color = AccentSoft, fontWeight = FontWeight.Bold) }
                    else TextButton(onClick = { adding = true },
                        modifier = Modifier.tvFocus(RoundedCornerShape(8.dp))) {
                        Text("+ Νέο προφίλ", color = AccentSoft)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { if (adding) adding = false else dialog = "" },
                        modifier = Modifier.tvFocus(RoundedCornerShape(8.dp))) {
                        Text(if (adding) "Πίσω" else "Κλείσιμο", color = TextMid)
                    }
                }
            )
        }
        "backup" -> {
            var passwordMode by remember { mutableStateOf("") }
            var backupPassword by remember { mutableStateOf("") }
            val passwordFocus = rememberInitialFocus()
            AlertDialog(
                onDismissRequest = { dialog = "" },
                containerColor = BgElev2,
                title = {
                    Text(
                        if (passwordMode.isBlank()) "Ασφαλές αντίγραφο"
                        else if (passwordMode == "export") "Κωδικός εξαγωγής" else "Κωδικός επαναφοράς",
                        color = TextHi
                    )
                },
                text = {
                    if (passwordMode.isBlank()) {
                        Column {
                            val fB = rememberInitialFocus()
                            Text(
                                "Όλα τα δεδομένα — μαζί με λίστες και credentials — κρυπτογραφούνται με AES-256. Ο κωδικός δεν αποθηκεύεται και δεν μπορεί να ανακτηθεί.",
                                color = TextMid, fontSize = 12.sp, lineHeight = 17.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { passwordMode = "export" },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                modifier = Modifier.fillMaxWidth()
                                    .focusRequester(fB).tvFocus(RoundedCornerShape(12.dp), tint = false)
                            ) { Text("Εξαγωγή κρυπτογραφημένου αρχείου", fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { passwordMode = "import" },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Line),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextHi),
                                modifier = Modifier.fillMaxWidth().tvFocus(RoundedCornerShape(12.dp))
                            ) { Text("Επαναφορά από αρχείο") }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Χρησιμοποίησε κωδικό τουλάχιστον 6 χαρακτήρων. Για παλιά μη κρυπτογραφημένα backups ο κωδικός αγνοείται.",
                                color = TextLo, fontSize = 11.sp, lineHeight = 15.sp
                            )
                        }
                    } else {
                        Column {
                            Text(
                                if (passwordMode == "export")
                                    "Θα χρειαστείς ακριβώς τον ίδιο κωδικό για επαναφορά."
                                else "Δώσε τον κωδικό με τον οποίο δημιουργήθηκε το αρχείο.",
                                color = TextMid, fontSize = 12.sp, lineHeight = 17.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            SettingField(
                                "Κωδικός backup", backupPassword, isPassword = true,
                                modifier = Modifier.focusRequester(passwordFocus)
                            ) { backupPassword = it }
                        }
                    }
                },
                confirmButton = {
                    if (passwordMode.isNotBlank()) {
                        TextButton(
                            enabled = backupPassword.length >= 6,
                            onClick = {
                                val password = backupPassword
                                dialog = ""
                                if (passwordMode == "export") onExportBackup(password)
                                else onImportBackup(password)
                            },
                            modifier = Modifier.tvFocus(RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                if (passwordMode == "export") "Εξαγωγή" else "Επιλογή αρχείου",
                                color = AccentSoft, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            if (passwordMode.isBlank()) dialog = ""
                            else { passwordMode = ""; backupPassword = "" }
                        },
                        modifier = Modifier.tvFocus(RoundedCornerShape(8.dp))
                    ) { Text(if (passwordMode.isBlank()) "Κλείσιμο" else "Πίσω", color = TextMid) }
                }
            )
        }
        "pin" -> {
            var newPin by remember { mutableStateOf("") }
            val fPin = rememberInitialFocus()
            AlertDialog(
                onDismissRequest = { dialog = "" }, containerColor = BgElev2,
                title = { Text("Γονικός έλεγχος", color = TextHi) },
                text = {
                    Column {
                        Text(
                            if (vm.hasParentalPin()) "Υπάρχει PIN. Δώσε νέο για αλλαγή."
                            else "Όρισε 4-6ψήφιο PIN. Μετά: κράτησε πατημένο ένα group στη λίστα για να το κλειδώσεις 🔒.",
                            color = TextMid, fontSize = 12.sp, lineHeight = 17.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        SettingField("PIN", newPin, isPassword = true,
                            modifier = Modifier.focusRequester(fPin)) {
                            if (it.length <= 6 && it.all(Char::isDigit)) newPin = it
                        }
                    }
                },
                confirmButton = {
                    TextButton(enabled = newPin.length >= 4,
                        onClick = { vm.setParentalPin(newPin); toast(ctx, "✓ Το PIN ορίστηκε"); dialog = "" },
                        modifier = Modifier.tvFocus(RoundedCornerShape(8.dp))) {
                        Text("Αποθήκευση", color = AccentSoft, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialog = "" },
                        modifier = Modifier.tvFocus(RoundedCornerShape(8.dp))) {
                        Text("Άκυρο", color = TextMid)
                    }
                }
            )
        }
    }
}
