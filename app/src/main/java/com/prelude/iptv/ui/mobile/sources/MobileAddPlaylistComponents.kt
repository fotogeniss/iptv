package com.prelude.iptv.ui.mobile.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    error: String? = null,
    focusRequester: FocusRequester? = null,
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
            modifier = Modifier.fillMaxWidth().then(
                if (focusRequester == null) Modifier else Modifier.focusRequester(focusRequester),
            ),
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
            isError = error != null,
            supportingText = error?.let { message ->
                { Text(message, color = IptvColors.Error, fontSize = 10.sp, lineHeight = 14.sp) }
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
                errorBorderColor = IptvColors.Error,
                errorCursorColor = IptvColors.Error,
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
