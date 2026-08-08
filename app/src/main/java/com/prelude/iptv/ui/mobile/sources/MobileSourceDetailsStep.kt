package com.prelude.iptv.ui.mobile.sources

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.sources.PlaylistSourceDraft
import com.prelude.iptv.ui.sources.PlaylistSourceDraftPolicy
import com.prelude.iptv.ui.sources.PlaylistSourceField
import com.prelude.iptv.ui.sources.PlaylistSourceMethod
import com.prelude.iptv.ui.sources.PlaylistSourceValidation

@Composable
internal fun MobileSourceDetailsStep(
    draft: PlaylistSourceDraft,
    validation: PlaylistSourceValidation?,
    generalError: String?,
    advancedExpanded: Boolean,
    passwordVisible: Boolean,
    importingFile: Boolean,
    onDraftChange: (PlaylistSourceDraft) -> Unit,
    onAdvancedChange: (Boolean) -> Unit,
    onTogglePassword: () -> Unit,
    onChangeMethod: () -> Unit,
    onPickFile: () -> Unit,
    onSubmit: () -> Unit,
    onExit: () -> Unit,
) {
    val content = methodContent(draft.method)
    val fieldFocus = remember { PlaylistSourceField.entries.associateWith { FocusRequester() } }
    LaunchedEffect(validation?.field) {
        validation?.field?.let { field ->
            if (field != PlaylistSourceField.FILE) fieldFocus.getValue(field).requestFocus()
        }
    }
    Column(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 18.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(IptvColors.Surface.copy(alpha = .82f))
                .border(1.dp, IptvColors.DividerStrong, RoundedCornerShape(16.dp))
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PremiumSourceIcon(content.icon, selected = true, size = 42)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(content.summaryTitle, color = IptvColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(content.summaryText, color = IptvColors.TextSecondary, fontSize = 9.5.sp, lineHeight = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onChangeMethod) {
                Text("Αλλαγή", color = IptvColors.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(top = 18.dp).clip(RoundedCornerShape(20.dp))
                .background(IptvColors.Surface.copy(alpha = .84f))
                .border(1.dp, IptvColors.DividerStrong, RoundedCornerShape(20.dp))
                .padding(19.dp),
        ) {
            Text(content.title, color = IptvColors.TextPrimary, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black)
            Text(content.subtitle, color = IptvColors.TextSecondary, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 7.dp, bottom = 20.dp))

            when (draft.method) {
                PlaylistSourceMethod.URL -> MobilePlaylistField(
                    label = "Σύνδεσμος λίστας",
                    value = draft.playlistUrl,
                    placeholder = "http://example.com/playlist.m3u",
                    onValueChange = { onDraftChange(draft.copy(playlistUrl = it)) },
                    error = validation.forField(PlaylistSourceField.PLAYLIST_URL),
                    focusRequester = fieldFocus.getValue(PlaylistSourceField.PLAYLIST_URL),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                )

                PlaylistSourceMethod.XTREAM -> {
                    MobilePlaylistField(
                        label = "Διεύθυνση server",
                        value = draft.server,
                        placeholder = "http://example.com:8080",
                        onValueChange = { onDraftChange(draft.copy(server = it)) },
                        error = validation.forField(PlaylistSourceField.SERVER),
                        focusRequester = fieldFocus.getValue(PlaylistSourceField.SERVER),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(13.dp))
                    MobilePlaylistField(
                        label = "Όνομα χρήστη",
                        value = draft.username,
                        placeholder = "Username",
                        onValueChange = { onDraftChange(draft.copy(username = it)) },
                        error = validation.forField(PlaylistSourceField.USERNAME),
                        focusRequester = fieldFocus.getValue(PlaylistSourceField.USERNAME),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(13.dp))
                    MobilePlaylistField(
                        label = "Κωδικός",
                        value = draft.password,
                        placeholder = "Password",
                        onValueChange = { onDraftChange(draft.copy(password = it)) },
                        password = true,
                        passwordVisible = passwordVisible,
                        onTogglePassword = onTogglePassword,
                        error = validation.forField(PlaylistSourceField.PASSWORD),
                        focusRequester = fieldFocus.getValue(PlaylistSourceField.PASSWORD),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    )
                }

                PlaylistSourceMethod.MAC -> {
                    MobilePlaylistField(
                        label = "Διεύθυνση portal",
                        value = draft.portal,
                        placeholder = "http://portal.example.com/c/",
                        onValueChange = { onDraftChange(draft.copy(portal = it)) },
                        error = validation.forField(PlaylistSourceField.PORTAL),
                        focusRequester = fieldFocus.getValue(PlaylistSourceField.PORTAL),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(13.dp))
                    MobilePlaylistField(
                        label = "MAC address",
                        value = draft.macAddress,
                        placeholder = "00:1A:79:00:00:00",
                        onValueChange = { onDraftChange(draft.copy(macAddress = PlaylistSourceDraftPolicy.formatMac(it))) },
                        error = validation.forField(PlaylistSourceField.MAC_ADDRESS),
                        focusRequester = fieldFocus.getValue(PlaylistSourceField.MAC_ADDRESS),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(13.dp))
                    MobilePlaylistField(
                        label = "User-Agent",
                        value = draft.userAgent,
                        placeholder = "MAG250 / custom User-Agent",
                        onValueChange = { onDraftChange(draft.copy(userAgent = it)) },
                        optional = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                }

                PlaylistSourceMethod.FILE -> MobilePremiumFilePicker(
                    fileLabel = draft.fileLabel,
                    importing = importingFile,
                    error = validation.forField(PlaylistSourceField.FILE),
                    onPick = onPickFile,
                )
            }

            Spacer(Modifier.height(7.dp))
            MobilePlaylistAdvancedToggle(expanded = advancedExpanded, onClick = { onAdvancedChange(!advancedExpanded) })
            AnimatedVisibility(
                visible = advancedExpanded,
                enter = fadeIn() + slideInVertically { -it / 5 },
                exit = fadeOut() + slideOutVertically { -it / 5 },
            ) {
                Column {
                    Spacer(Modifier.height(5.dp))
                    MobilePlaylistField(
                        label = "Όνομα πηγής",
                        value = draft.name,
                        placeholder = "π.χ. Η λίστα μου",
                        onValueChange = { onDraftChange(draft.copy(name = it)) },
                        optional = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )
                    if (draft.method != PlaylistSourceMethod.FILE) {
                        Spacer(Modifier.height(13.dp))
                        MobilePlaylistField(
                            label = "Οδηγός προγράμματος",
                            value = draft.epgUrl,
                            placeholder = "EPG URL",
                            onValueChange = { onDraftChange(draft.copy(epgUrl = it)) },
                            optional = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        )
                    }
                }
            }

            generalError?.let { message ->
                Text(message, color = IptvColors.Error, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 12.dp))
            }
            Spacer(Modifier.height(15.dp))
            MobileSourceSecurityNote()
            Spacer(Modifier.height(17.dp))
            Button(
                onClick = onSubmit,
                enabled = !importingFile,
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IptvColors.Primary,
                    contentColor = Color.White,
                    disabledContainerColor = IptvColors.SurfaceRaised,
                    disabledContentColor = IptvColors.TextTertiary,
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text("Έλεγχος και προσθήκη", fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
            TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth().height(45.dp)) {
                Text("Έξοδος", color = IptvColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MobilePremiumFilePicker(
    fileLabel: String,
    importing: Boolean,
    error: String?,
    onPick: () -> Unit,
) {
    Column {
        Text("Αρχείο M3U ή M3U8", color = IptvColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(7.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                .background(IptvColors.SurfaceRaised)
                .border(1.dp, if (error == null) IptvColors.DividerStrong else IptvColors.Error, RoundedCornerShape(13.dp))
                .clickable(enabled = !importing, onClick = onPick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PremiumSourceIcon(Icons.AutoMirrored.Filled.InsertDriveFile, size = 40)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        importing -> "Εισαγωγή αρχείου…"
                        fileLabel.isNotBlank() -> fileLabel
                        else -> "Επίλεξε αρχείο από τη συσκευή"
                    },
                    color = if (fileLabel.isBlank()) IptvColors.TextTertiary else IptvColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = if (fileLabel.isBlank()) FontWeight.Normal else FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("Υποστηρίζονται .m3u και .m3u8", color = IptvColors.TextTertiary, fontSize = 8.5.sp, modifier = Modifier.padding(top = 3.dp))
            }
            if (fileLabel.isNotBlank()) Icon(Icons.Default.CheckCircle, null, tint = IptvColors.Success, modifier = Modifier.size(19.dp))
        }
        error?.let { Text(it, color = IptvColors.Error, fontSize = 10.sp, modifier = Modifier.padding(start = 3.dp, top = 5.dp)) }
    }
}

private data class MobileMethodContent(
    val icon: ImageVector,
    val summaryTitle: String,
    val summaryText: String,
    val title: String,
    val subtitle: String,
)

private fun methodContent(method: PlaylistSourceMethod): MobileMethodContent = when (method) {
    PlaylistSourceMethod.URL -> MobileMethodContent(Icons.Default.Link, "Σύνδεσμος λίστας", "Θα ελέγξουμε ότι η λίστα απαντά πριν αποθηκευτεί.", "Συμπλήρωσε τον σύνδεσμο", "Επικόλλησε τον σύνδεσμο ακριβώς όπως σου τον έστειλε ο πάροχος.")
    PlaylistSourceMethod.XTREAM -> MobileMethodContent(Icons.Default.Lock, "Server και κωδικοί", "Χρειαζόμαστε server, username και password.", "Συμπλήρωσε τα στοιχεία σύνδεσης", "Αντέγραψε τα τρία στοιχεία ακριβώς όπως εμφανίζονται στο μήνυμα του παρόχου.")
    PlaylistSourceMethod.MAC -> MobileMethodContent(Icons.Default.Dns, "Portal και MAC", "Η MAC address μορφοποιείται αυτόματα.", "Συμπλήρωσε Portal και MAC", "Χρησιμοποίησε το Portal URL και τη MAC address που σου έδωσε ο πάροχος.")
    PlaylistSourceMethod.FILE -> MobileMethodContent(Icons.Default.FolderOpen, "Τοπικό αρχείο", "Το αρχείο διαβάζεται μόνο σε αυτή τη συσκευή.", "Διάλεξε το αρχείο σου", "Υποστηρίζονται αρχεία M3U και M3U8 από τη συσκευή.")
}

private fun PlaylistSourceValidation?.forField(field: PlaylistSourceField): String? =
    this?.message?.takeIf { this.field == field }
