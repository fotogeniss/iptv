package com.prelude.iptv.ui.tv.sources

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.sources.PlaylistSourceDraft
import com.prelude.iptv.ui.sources.PlaylistSourceField
import com.prelude.iptv.ui.sources.PlaylistSourceMethod
import com.prelude.iptv.ui.sources.PlaylistSourceValidation

@Composable
internal fun TvSourceDetailsStep(
    draft: PlaylistSourceDraft,
    validation: PlaylistSourceValidation?,
    generalError: String?,
    advancedExpanded: Boolean,
    importingFile: Boolean,
    inputFocus: Map<TvPlaylistInput, FocusRequester>,
    changeFocus: FocusRequester,
    advancedFocus: FocusRequester,
    exitFocus: FocusRequester,
    submitFocus: FocusRequester,
    onChangeMethod: () -> Unit,
    onEditInput: (TvPlaylistInput) -> Unit,
    onAdvancedChange: (Boolean) -> Unit,
    onPickFile: () -> Unit,
    onExit: () -> Unit,
    onSubmit: () -> Unit,
) {
    val content = tvMethodContent(draft.method)
    val firstInput = inputFocus.getValue(firstInputFor(draft.method))
    Row(
        Modifier.fillMaxWidth().fillMaxHeight().padding(top = 30.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(34.dp),
    ) {
        Column(
            Modifier.weight(.68f).fillMaxHeight().clip(RoundedCornerShape(19.dp))
                .background(IptvColors.Surface.copy(alpha = .8f))
                .border(1.dp, IptvColors.DividerStrong, RoundedCornerShape(19.dp))
                .padding(24.dp),
        ) {
            TvPremiumSourceIcon(content.icon)
            Text(content.summaryTitle, color = IptvColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 19.dp))
            Text(content.summaryText, color = IptvColors.TextSecondary, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 7.dp))
            TvPlaylistAction(
                label = "Αλλαγή τρόπου",
                primary = false,
                focusRequester = changeFocus,
                modifier = Modifier.fillMaxWidth().padding(top = 21.dp).focusProperties { right = firstInput },
                onClick = onChangeMethod,
            )
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = .025f))
                    .border(1.dp, IptvColors.DividerStrong, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Default.Security, null, tint = IptvColors.Success, modifier = Modifier.size(18.dp))
                Text(
                    "Τα στοιχεία αποθηκεύονται κρυπτογραφημένα σε αυτή τη συσκευή.",
                    color = IptvColors.TextSecondary,
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(start = 9.dp),
                )
            }
        }

        TvPlaylistFormCard(
            title = content.title,
            subtitle = content.subtitle,
            modifier = Modifier.weight(1.32f).fillMaxHeight(),
        ) {
            when (draft.method) {
                PlaylistSourceMethod.URL -> TvPlaylistFieldButton(
                    label = "Σύνδεσμος λίστας",
                    value = draft.playlistUrl,
                    placeholder = "http://example.com/playlist.m3u",
                    focusRequester = inputFocus.getValue(TvPlaylistInput.PLAYLIST_URL),
                    modifier = Modifier.focusProperties { left = changeFocus },
                    error = validation.forField(PlaylistSourceField.PLAYLIST_URL),
                    onClick = { onEditInput(TvPlaylistInput.PLAYLIST_URL) },
                )

                PlaylistSourceMethod.XTREAM -> {
                    TvPlaylistFieldButton(
                        label = "Διεύθυνση server",
                        value = draft.server,
                        placeholder = "http://example.com:8080",
                        focusRequester = inputFocus.getValue(TvPlaylistInput.SERVER),
                        modifier = Modifier.focusProperties { left = changeFocus },
                        error = validation.forField(PlaylistSourceField.SERVER),
                        onClick = { onEditInput(TvPlaylistInput.SERVER) },
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TvPlaylistFieldButton(
                            label = "Όνομα χρήστη",
                            value = draft.username,
                            placeholder = "Username",
                            focusRequester = inputFocus.getValue(TvPlaylistInput.USERNAME),
                            modifier = Modifier.weight(1f),
                            error = validation.forField(PlaylistSourceField.USERNAME),
                            onClick = { onEditInput(TvPlaylistInput.USERNAME) },
                        )
                        TvPlaylistFieldButton(
                            label = "Κωδικός",
                            value = draft.password,
                            placeholder = "Password",
                            focusRequester = inputFocus.getValue(TvPlaylistInput.PASSWORD),
                            modifier = Modifier.weight(1f),
                            password = true,
                            error = validation.forField(PlaylistSourceField.PASSWORD),
                            onClick = { onEditInput(TvPlaylistInput.PASSWORD) },
                        )
                    }
                }

                PlaylistSourceMethod.MAC -> {
                    TvPlaylistFieldButton(
                        label = "Διεύθυνση portal",
                        value = draft.portal,
                        placeholder = "http://portal.example.com/c/",
                        focusRequester = inputFocus.getValue(TvPlaylistInput.PORTAL),
                        modifier = Modifier.focusProperties { left = changeFocus },
                        error = validation.forField(PlaylistSourceField.PORTAL),
                        onClick = { onEditInput(TvPlaylistInput.PORTAL) },
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TvPlaylistFieldButton(
                            label = "MAC address",
                            value = draft.macAddress,
                            placeholder = "00:1A:79:00:00:00",
                            focusRequester = inputFocus.getValue(TvPlaylistInput.MAC),
                            modifier = Modifier.weight(1f),
                            error = validation.forField(PlaylistSourceField.MAC_ADDRESS),
                            onClick = { onEditInput(TvPlaylistInput.MAC) },
                        )
                        TvPlaylistFieldButton(
                            label = "User-Agent",
                            value = draft.userAgent,
                            placeholder = "MAG250 / User-Agent",
                            focusRequester = inputFocus.getValue(TvPlaylistInput.USER_AGENT),
                            modifier = Modifier.weight(1f),
                            optional = true,
                            onClick = { onEditInput(TvPlaylistInput.USER_AGENT) },
                        )
                    }
                }

                PlaylistSourceMethod.FILE -> TvM3uFileButton(
                    fileLabel = draft.fileLabel,
                    importing = importingFile,
                    focusRequester = inputFocus.getValue(TvPlaylistInput.FILE),
                    modifier = Modifier.focusProperties { left = changeFocus },
                    onClick = onPickFile,
                )
            }

            Spacer(Modifier.height(6.dp))
            TvPlaylistAdvancedButton(
                expanded = advancedExpanded,
                focusRequester = advancedFocus,
                modifier = Modifier.focusProperties { down = submitFocus },
                onClick = { onAdvancedChange(!advancedExpanded) },
            )
            AnimatedVisibility(visible = advancedExpanded, enter = fadeIn(), exit = fadeOut()) {
                Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvPlaylistFieldButton(
                        label = "Όνομα πηγής",
                        value = draft.name,
                        placeholder = "π.χ. Η λίστα μου",
                        focusRequester = inputFocus.getValue(TvPlaylistInput.NAME),
                        modifier = Modifier.weight(1f),
                        optional = true,
                        onClick = { onEditInput(TvPlaylistInput.NAME) },
                    )
                    if (draft.method != PlaylistSourceMethod.FILE) {
                        TvPlaylistFieldButton(
                            label = "Οδηγός προγράμματος",
                            value = draft.epgUrl,
                            placeholder = "EPG URL",
                            focusRequester = inputFocus.getValue(TvPlaylistInput.EPG),
                            modifier = Modifier.weight(1f),
                            optional = true,
                            onClick = { onEditInput(TvPlaylistInput.EPG) },
                        )
                    }
                }
            }
            generalError?.let { message ->
                Text(message, color = IptvColors.Error, fontSize = 9.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.End) {
                TvPlaylistAction(
                    label = "Έξοδος",
                    primary = false,
                    focusRequester = exitFocus,
                    modifier = Modifier.width(150.dp).focusProperties { right = submitFocus; up = advancedFocus },
                    onClick = onExit,
                )
                Spacer(Modifier.width(12.dp))
                TvPlaylistAction(
                    label = "Έλεγχος και προσθήκη",
                    primary = true,
                    focusRequester = submitFocus,
                    enabled = !importingFile,
                    modifier = Modifier.width(245.dp).focusProperties { left = exitFocus; up = advancedFocus },
                    onClick = onSubmit,
                )
            }
        }
    }
}

@Composable
private fun TvPremiumSourceIcon(icon: ImageVector) {
    androidx.compose.foundation.layout.Box(
        Modifier.width(58.dp).height(58.dp).clip(RoundedCornerShape(16.dp))
            .background(IptvColors.Primary.copy(alpha = .16f))
            .border(1.dp, IptvColors.Primary.copy(alpha = .36f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = IptvColors.TextPrimary, modifier = Modifier.size(29.dp))
    }
}

internal enum class TvPlaylistInput(val title: String) {
    PLAYLIST_URL("Σύνδεσμος λίστας"),
    SERVER("Διεύθυνση server"),
    USERNAME("Όνομα χρήστη"),
    PASSWORD("Κωδικός"),
    PORTAL("Διεύθυνση portal"),
    MAC("MAC address"),
    USER_AGENT("User-Agent"),
    FILE("Αρχείο M3U"),
    NAME("Όνομα πηγής"),
    EPG("Οδηγός προγράμματος"),
}

internal fun firstInputFor(method: PlaylistSourceMethod): TvPlaylistInput = when (method) {
    PlaylistSourceMethod.URL -> TvPlaylistInput.PLAYLIST_URL
    PlaylistSourceMethod.XTREAM -> TvPlaylistInput.SERVER
    PlaylistSourceMethod.MAC -> TvPlaylistInput.PORTAL
    PlaylistSourceMethod.FILE -> TvPlaylistInput.FILE
}

internal fun invalidInput(validation: PlaylistSourceValidation?): TvPlaylistInput? = when (validation?.field) {
    PlaylistSourceField.PLAYLIST_URL -> TvPlaylistInput.PLAYLIST_URL
    PlaylistSourceField.SERVER -> TvPlaylistInput.SERVER
    PlaylistSourceField.USERNAME -> TvPlaylistInput.USERNAME
    PlaylistSourceField.PASSWORD -> TvPlaylistInput.PASSWORD
    PlaylistSourceField.PORTAL -> TvPlaylistInput.PORTAL
    PlaylistSourceField.MAC_ADDRESS -> TvPlaylistInput.MAC
    PlaylistSourceField.FILE -> TvPlaylistInput.FILE
    null -> null
}

internal fun inputValue(input: TvPlaylistInput, draft: PlaylistSourceDraft): String = when (input) {
    TvPlaylistInput.PLAYLIST_URL -> draft.playlistUrl
    TvPlaylistInput.SERVER -> draft.server
    TvPlaylistInput.USERNAME -> draft.username
    TvPlaylistInput.PASSWORD -> draft.password
    TvPlaylistInput.PORTAL -> draft.portal
    TvPlaylistInput.MAC -> draft.macAddress
    TvPlaylistInput.USER_AGENT -> draft.userAgent
    TvPlaylistInput.NAME -> draft.name
    TvPlaylistInput.EPG -> draft.epgUrl
    TvPlaylistInput.FILE -> ""
}

private fun PlaylistSourceValidation?.forField(field: PlaylistSourceField): String? =
    this?.message?.takeIf { this.field == field }

private data class TvMethodFormContent(
    val icon: ImageVector,
    val summaryTitle: String,
    val summaryText: String,
    val title: String,
    val subtitle: String,
)

private fun tvMethodContent(method: PlaylistSourceMethod): TvMethodFormContent = when (method) {
    PlaylistSourceMethod.URL -> TvMethodFormContent(Icons.Default.Link, "Σύνδεσμος λίστας", "Θα ελέγξουμε ότι η λίστα απαντά πριν αποθηκευτεί.", "Συμπλήρωσε τον σύνδεσμο", "Επικόλλησε τον σύνδεσμο ακριβώς όπως σου τον έστειλε ο πάροχος.")
    PlaylistSourceMethod.XTREAM -> TvMethodFormContent(Icons.Default.Lock, "Server και κωδικοί", "Χρειαζόμαστε server, username και password.", "Συμπλήρωσε τα στοιχεία σύνδεσης", "Αντέγραψε τα τρία στοιχεία ακριβώς όπως εμφανίζονται στο μήνυμα του παρόχου.")
    PlaylistSourceMethod.MAC -> TvMethodFormContent(Icons.Default.Dns, "Portal και MAC", "Η MAC address μορφοποιείται αυτόματα.", "Συμπλήρωσε Portal και MAC", "Χρησιμοποίησε το Portal URL και τη MAC address που σου έδωσε ο πάροχος.")
    PlaylistSourceMethod.FILE -> TvMethodFormContent(Icons.Default.FolderOpen, "Τοπικό αρχείο", "Το αρχείο διαβάζεται μόνο σε αυτή τη συσκευή.", "Διάλεξε το αρχείο σου", "Υποστηρίζονται αρχεία M3U και M3U8 από τη συσκευή.")
}
