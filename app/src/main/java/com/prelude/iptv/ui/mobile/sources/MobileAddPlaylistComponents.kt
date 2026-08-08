package com.prelude.iptv.ui.mobile.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors

@Composable
internal fun MobilePlaylistBrand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(IptvColors.Primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("PRELUDE", color = IptvColors.TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black)
        Text("+", color = IptvColors.Primary, fontSize = 21.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun MobilePlaylistMethodCard(
    method: MobilePlaylistMethod,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val content = when (method) {
        MobilePlaylistMethod.URL -> MethodContent(Icons.Default.Link, "URL", "M3U", "Επικόλληση συνδέσμου playlist")
        MobilePlaylistMethod.XTREAM -> MethodContent(Icons.Default.Lock, "Όνομα χρήστη & κωδικός", "XTREAM", "Server, username και password")
        MobilePlaylistMethod.MAC -> MethodContent(Icons.Default.Dns, "MAC Portal", "STALKER", "Portal URL και MAC address")
        MobilePlaylistMethod.FILE -> MethodContent(Icons.Default.FolderOpen, "Αρχείο M3U", "FILE", "Επιλογή αρχείου από τη συσκευή")
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected) Color.White.copy(alpha = .09f) else IptvColors.Surface.copy(alpha = .78f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.White.copy(alpha = .72f) else IptvColors.DividerStrong,
                shape = RoundedCornerShape(15.dp),
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Color.White.copy(alpha = .055f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(content.icon, null, tint = if (selected) Color.White else IptvColors.TextSecondary, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // weight(1f, fill = false): ο τίτλος παίρνει όσο χώρο χρειάζεται
                // αλλά ΥΠΟΧΩΡΕΙ πρώτος όταν δεν χωράει και τα δύο — χωρίς αυτό,
                // ο μακρύς τίτλος «Όνομα χρήστη & κωδικός» έτρωγε όλο τον χώρο
                // της γραμμής και το «XTREAM» δίπλα του τσαλακωνόταν σε ένα-δύο
                // ορατά γράμματα («XTREA»).
                Text(
                    content.title,
                    color = if (selected) IptvColors.TextPrimary else IptvColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    content.tag,
                    color = IptvColors.TextSecondary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = .07f))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
            Text(
                content.subtitle,
                color = IptvColors.TextTertiary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(24.dp).border(2.dp, if (selected) Color.White else IptvColors.TextTertiary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(12.dp).background(Color.White, CircleShape))
        }
    }
}

@Composable
internal fun MobilePlaylistField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    optional: Boolean = false,
    password: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = IptvColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            if (optional) Text("Προαιρετικό", color = IptvColors.TextTertiary, fontSize = 9.sp)
        }
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(13.dp),
            placeholder = { Text(placeholder, color = IptvColors.TextTertiary, fontSize = 13.sp) },
            visualTransformation = if (password && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (!password || onTogglePassword == null) null else {
                {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Απόκρυψη κωδικού" else "Εμφάνιση κωδικού",
                            tint = IptvColors.TextSecondary,
                        )
                    }
                }
            },
            keyboardOptions = keyboardOptions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = IptvColors.TextPrimary,
                unfocusedTextColor = IptvColors.TextPrimary,
                cursorColor = IptvColors.Primary,
                focusedBorderColor = IptvColors.Primary,
                unfocusedBorderColor = IptvColors.DividerStrong,
                focusedContainerColor = IptvColors.SurfaceRaised,
                unfocusedContainerColor = IptvColors.SurfaceRaised,
            ),
        )
    }
}

@Composable
internal fun MobilePlaylistAdvancedToggle(expanded: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(9.dp)).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.KeyboardArrowDown,
            null,
            tint = IptvColors.TextSecondary,
            modifier = Modifier.size(18.dp).rotate(if (expanded) 180f else 0f),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            if (expanded) "Λιγότερες επιλογές" else "Περισσότερες επιλογές",
            color = IptvColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun MobilePlaylistQuickTip(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IptvColors.Surface,
        titleContentColor = IptvColors.TextPrimary,
        textContentColor = IptvColors.TextSecondary,
        title = { Text("Πού βρίσκω τα στοιχεία;", fontWeight = FontWeight.Black) },
        text = {
            Text(
                "Χρησιμοποίησε τα στοιχεία που σου έχει δώσει ο νόμιμος πάροχος περιεχομένου. " +
                    "Για M3U χρειάζεσαι URL, για Xtream server, username και password, ενώ για " +
                    "MAC/Stalker χρειάζεσαι Portal URL και MAC address.",
                lineHeight = 20.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Κατάλαβα", color = IptvColors.Primary, fontWeight = FontWeight.Bold) }
        },
    )
}

private data class MethodContent(
    val icon: ImageVector,
    val title: String,
    val tag: String,
    val subtitle: String,
)
