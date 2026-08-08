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
import androidx.compose.ui.res.stringResource
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
internal fun StepBtn(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(BgElev)
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .tvFocus(RoundedCornerShape(12.dp))    // χωρίς αυτό, αόρατο focus σε TV
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(label, color = TextHi, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingField(
    label: String, value: String, isPassword: Boolean = false,
    modifier: Modifier = Modifier, onChange: (String) -> Unit
) {
    // Τηλεόραση: το πληκτρολόγιο ανοίγει μόνο με OK, όχι μόλις περάσει το focus.
    if (isTvDevice()) TvSettingField(label, value, isPassword, modifier, onChange)
    else PhoneSettingField(label, value, isPassword, modifier, onChange)
}

@Composable
private fun TvSettingField(
    label: String, value: String, isPassword: Boolean,
    modifier: Modifier = Modifier, onChange: (String) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = TextLo, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
        Box(
            modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BgElev)
                .border(1.dp, Line, RoundedCornerShape(12.dp))
                .tvFocus(RoundedCornerShape(12.dp))
                .clickable { editing = true }
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            val shown = when {
                value.isEmpty() -> "—"
                isPassword -> "•".repeat(value.length.coerceAtMost(14))
                else -> value
            }
            Text(
                shown, color = if (value.isEmpty()) TextLo else TextHi, fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
    if (editing) TextEntryDialog(
        title = label,
        initial = value,
        isPassword = isPassword,
        onDismiss = { editing = false },
        onOk = { onChange(it); editing = false }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneSettingField(
    label: String, value: String, isPassword: Boolean,
    modifier: Modifier = Modifier, onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation()
        else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextHi, unfocusedTextColor = TextHi,
            focusedBorderColor = Accent, unfocusedBorderColor = Line,
            focusedLabelColor = AccentSoft, unfocusedLabelColor = TextLo,
            unfocusedContainerColor = BgElev, focusedContainerColor = BgElev,
            cursorColor = Accent
        ),
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

/** 4ψήφιο PIN γονικού ελέγχου — με αρχικό focus για TV. */
@Composable
internal fun PinDialog(title: String, onOk: (String) -> Unit, onCancel: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    val f = rememberInitialFocus()
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = BgElev2,
        title = { Text(title, color = TextHi) },
        text = {
            SettingField("PIN", pin, isPassword = true, modifier = Modifier.focusRequester(f)) {
                if (it.length <= 6 && it.all(Char::isDigit)) pin = it
            }
        },
        confirmButton = {
            TextButton(enabled = pin.length >= 4, onClick = { onOk(pin) },
                modifier = Modifier.tvFocus(RoundedCornerShape(8.dp))) {
                Text(stringResource(R.string.browse_confirm), color = AccentSoft, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.tvFocus(RoundedCornerShape(8.dp))) {
                Text(stringResource(R.string.browse_cancel), color = TextMid)
            }
        }
    )
}
