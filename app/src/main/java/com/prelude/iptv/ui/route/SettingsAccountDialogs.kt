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
import com.prelude.iptv.R
import com.prelude.iptv.data.*
import com.prelude.iptv.ui.*
import com.prelude.iptv.ui.components.library.*
import com.prelude.iptv.ui.design.*
import com.prelude.iptv.ui.localization.localizedProfileName
import com.prelude.iptv.ui.profile.ProfilePresentationPolicy
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
                toast(ctx, ctx.getString(R.string.account_profile_switch_restart))
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                        ?.putExtra(AppRouteContract.EXTRA_SKIP_PROFILE_GATE, true)
                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    if (launch != null) ctx.startActivity(launch)
                }, 350)
            }

            askPinFor?.let { id ->
                PinDialog(
                    title = stringResource(R.string.account_profile_switch_pin_title),
                    onOk = { pin ->
                        if (vm.checkPin(pin)) { askPinFor = null; dialog = ""; switchTo(id) }
                        else toast(ctx, ctx.getString(R.string.account_profile_wrong_pin))
                    },
                    onCancel = { askPinFor = null }
                )
            }

            AlertDialog(
                onDismissRequest = { dialog = "" }, containerColor = BgElev2,
                title = {
                    Text(
                        stringResource(
                            if (adding) R.string.account_profile_new_title
                            else R.string.account_profiles_title
                        ),
                        color = TextHi,
                    )
                },
                text = {
                    Column {
                        if (adding) {
                            val fN = rememberInitialFocus()
                            SettingField(
                                stringResource(R.string.account_profile_name_label),
                                newName,
                                modifier = Modifier.focusRequester(fN),
                            ) { newName = it }
                            Row(
                                Modifier.fillMaxWidth().tvFocus(RoundedCornerShape(8.dp))
                                    .clickable { newProtected = !newProtected }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = newProtected, onCheckedChange = { newProtected = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Accent))
                                Column {
                                    Text(stringResource(R.string.account_profile_pin_protection), color = TextHi, fontSize = 13.sp)
                                    Text(stringResource(R.string.account_profile_pin_protection_body),
                                        color = TextLo, fontSize = 11.sp)
                                }
                            }
                        } else {
                            Text(
                                stringResource(R.string.account_profiles_local_data_body),
                                color = TextMid, fontSize = 12.sp, lineHeight = 17.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            val fP = rememberInitialFocus()
                            profiles.forEachIndexed { i, p ->
                                val displayName = localizedProfileName(ProfilePresentationPolicy.displayName(p))
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
                                        (if (p.protected) "🔒 " else "") + displayName,
                                        color = if (p.id == active) TextHi else TextMid,
                                        fontWeight = if (p.id == active) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (p.id != 0) IconButton(onClick = {
                                        vm.deleteProfile(p.id)
                                        toast(ctx, ctx.getString(R.string.account_profile_deleted, p.name))
                                        dialog = ""
                                    }, modifier = Modifier.size(28.dp).tvFocus(RoundedCornerShape(6.dp))) {
                                        Icon(Icons.Default.Delete, stringResource(R.string.settings_delete), tint = TextLo,
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
                                toast(ctx, ctx.getString(R.string.account_profile_set_pin_first))
                            else { vm.addProfile(newName, newProtected); adding = false; newName = ""; newProtected = false }
                        },
                        modifier = Modifier.tvFocus(RoundedCornerShape(8.dp))
                    ) { Text(stringResource(R.string.account_profile_add), color = AccentSoft, fontWeight = FontWeight.Bold) }
                    else TextButton(onClick = { adding = true },
                        modifier = Modifier.tvFocus(RoundedCornerShape(8.dp))) {
                        Text(stringResource(R.string.account_profile_add_new), color = AccentSoft)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { if (adding) adding = false else dialog = "" },
                        modifier = Modifier.tvFocus(RoundedCornerShape(8.dp))) {
                        Text(
                            stringResource(if (adding) R.string.settings_back else R.string.settings_close),
                            color = TextMid,
                        )
                    }
                }
            )
        }
        "backup" -> {
            var passwordMode by remember { mutableStateOf<BackupPasswordMode?>(null) }
            var backupPassword by remember { mutableStateOf("") }
            val passwordFocus = rememberInitialFocus()
            AlertDialog(
                onDismissRequest = { dialog = "" },
                containerColor = BgElev2,
                title = {
                    Text(
                        stringResource(
                            when (passwordMode) {
                                null -> R.string.account_backup_title
                                BackupPasswordMode.Export -> R.string.account_backup_export_password_title
                                BackupPasswordMode.Restore -> R.string.account_backup_restore_password_title
                            }
                        ),
                        color = TextHi
                    )
                },
                text = {
                    if (passwordMode == null) {
                        Column {
                            val fB = rememberInitialFocus()
                            Text(
                                stringResource(R.string.account_backup_description),
                                color = TextMid, fontSize = 12.sp, lineHeight = 17.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { passwordMode = BackupPasswordMode.Export },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                modifier = Modifier.fillMaxWidth()
                                    .focusRequester(fB).tvFocus(RoundedCornerShape(12.dp), tint = false)
                            ) { Text(stringResource(R.string.account_backup_export_file), fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { passwordMode = BackupPasswordMode.Restore },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Line),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextHi),
                                modifier = Modifier.fillMaxWidth().tvFocus(RoundedCornerShape(12.dp))
                            ) { Text(stringResource(R.string.account_backup_restore_file)) }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                stringResource(R.string.account_backup_legacy_note),
                                color = TextLo, fontSize = 11.sp, lineHeight = 15.sp
                            )
                        }
                    } else {
                        Column {
                            Text(
                                stringResource(
                                    if (passwordMode == BackupPasswordMode.Export)
                                        R.string.account_backup_export_password_body
                                    else R.string.account_backup_restore_password_body
                                ),
                                color = TextMid, fontSize = 12.sp, lineHeight = 17.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            SettingField(
                                stringResource(R.string.account_backup_password_label), backupPassword, isPassword = true,
                                modifier = Modifier.focusRequester(passwordFocus)
                            ) { backupPassword = it }
                        }
                    }
                },
                confirmButton = {
                    if (passwordMode != null) {
                        TextButton(
                            enabled = backupPassword.length >= 6,
                            onClick = {
                                val password = backupPassword
                                dialog = ""
                                if (passwordMode == BackupPasswordMode.Export) onExportBackup(password)
                                else onImportBackup(password)
                            },
                            modifier = Modifier.tvFocus(RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                stringResource(
                                    if (passwordMode == BackupPasswordMode.Export) R.string.account_backup_export
                                    else R.string.account_backup_choose_file
                                ),
                                color = AccentSoft, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            if (passwordMode == null) dialog = ""
                            else { passwordMode = null; backupPassword = "" }
                        },
                        modifier = Modifier.tvFocus(RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            stringResource(if (passwordMode == null) R.string.settings_close else R.string.settings_back),
                            color = TextMid,
                        )
                    }
                }
            )
        }
        "pin" -> {
            var newPin by remember { mutableStateOf("") }
            val fPin = rememberInitialFocus()
            AlertDialog(
                onDismissRequest = { dialog = "" }, containerColor = BgElev2,
                title = { Text(stringResource(R.string.account_parental_title), color = TextHi) },
                text = {
                    Column {
                        Text(
                            stringResource(
                                if (vm.hasParentalPin()) R.string.account_parental_change_pin_body
                                else R.string.account_parental_create_pin_body
                            ),
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
                        onClick = {
                            vm.setParentalPin(newPin)
                            toast(ctx, ctx.getString(R.string.account_parental_pin_saved))
                            dialog = ""
                        },
                        modifier = Modifier.tvFocus(RoundedCornerShape(8.dp))) {
                        Text(stringResource(R.string.settings_save), color = AccentSoft, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialog = "" },
                        modifier = Modifier.tvFocus(RoundedCornerShape(8.dp))) {
                        Text(stringResource(R.string.settings_cancel), color = TextMid)
                    }
                }
            )
        }
    }
}

private enum class BackupPasswordMode {
    Export,
    Restore,
}
