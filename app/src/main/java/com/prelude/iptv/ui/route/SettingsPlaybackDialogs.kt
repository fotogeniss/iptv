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
import com.prelude.iptv.player.BufferProfile
import com.prelude.iptv.ui.*
import com.prelude.iptv.ui.components.settings.AutoFrameRateOption
import com.prelude.iptv.ui.components.settings.PlayerModeOption
import com.prelude.iptv.ui.components.library.*
import com.prelude.iptv.ui.design.*
import com.prelude.iptv.ui.localization.descriptionRes
import com.prelude.iptv.ui.localization.labelRes
import com.prelude.iptv.ui.localization.titleRes
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
            title = { Text(stringResource(R.string.settings_text_size), color = TextHi) },
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
                TvDialogTextButton(label = stringResource(R.string.settings_done), color = AccentSoft, onClick = { dialog = "" })
            }
        )
        "player" -> AlertDialog(
            onDismissRequest = { dialog = "" }, containerColor = BgElev2,
            title = { Text(stringResource(R.string.settings_player_mode_title), color = TextHi) },
            text = {
                Column {
                    val fPl = rememberInitialFocus()   // αλλιώς «νεκρό» dialog σε TV
                    PlayerModeOption.entries.forEachIndexed { fi, option ->
                        Row(
                            Modifier.fillMaxWidth()
                                .then(if (fi == 0) Modifier.focusRequester(fPl) else Modifier)
                                .tvFocus(RoundedCornerShape(8.dp)).clickable {
                                    playerMode = option.storageValue; store.playerMode = option.storageValue
                                }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = playerMode == option.storageValue, onClick = {
                                playerMode = option.storageValue; store.playerMode = option.storageValue
                            }, colors = RadioButtonDefaults.colors(selectedColor = Accent))
                            Text(stringResource(option.labelRes()), color = TextHi, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = { TvDialogTextButton(label = stringResource(R.string.settings_done), color = AccentSoft, onClick = { dialog = "" }) }
        )
        "buffer" -> AlertDialog(
            onDismissRequest = { dialog = "" }, containerColor = BgElev2,
            title = { Text(stringResource(R.string.settings_buffer_title), color = TextHi) },
            text = {
                Column {
                    val firstFocus = rememberInitialFocus()
                    BufferProfile.entries.forEachIndexed { index, profile ->
                        Row(
                            Modifier.fillMaxWidth()
                                .then(if (index == 0) Modifier.focusRequester(firstFocus) else Modifier)
                                .tvFocus(RoundedCornerShape(8.dp))
                                .clickable {
                                    bufferProfile = profile.storageValue
                                    store.bufferProfile = profile.storageValue
                                }
                                .padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = bufferProfile == profile.storageValue,
                                onClick = {
                                    bufferProfile = profile.storageValue
                                    store.bufferProfile = profile.storageValue
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Accent)
                            )
                            Column(Modifier.padding(start = 4.dp)) {
                                Text(stringResource(profile.titleRes()), color = TextHi, fontSize = 14.sp)
                                Text(stringResource(profile.descriptionRes()), color = TextMid, fontSize = 11.sp)
                            }
                        }
                    }
                    // Η ρύθμιση κλειδώνεται όταν χτίζεται ο player, οπότε δεν
                    // μπορεί να αλλάξει σε ροή που ήδη παίζει. Καλύτερα να το πει
                    // παρά να φανεί σαν να μην έκανε τίποτα.
                    Text(
                        stringResource(R.string.settings_buffer_applies_next),
                        color = TextMid,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            },
            confirmButton = {
                TvDialogTextButton(label = stringResource(R.string.settings_done), color = AccentSoft, onClick = { dialog = "" })
            }
        )
        "afr" -> AlertDialog(
            onDismissRequest = { dialog = "" }, containerColor = BgElev2,
            title = { Text(stringResource(R.string.settings_afr_title), color = TextHi) },
            text = {
                Column {
                    val firstFocus = rememberInitialFocus()
                    AutoFrameRateOption.entries.forEachIndexed { index, option ->
                        Row(
                            Modifier.fillMaxWidth()
                                .then(if (index == 0) Modifier.focusRequester(firstFocus) else Modifier)
                                .tvFocus(RoundedCornerShape(8.dp))
                                .clickable {
                                    autoFrameRateMode = option.storageValue
                                    store.autoFrameRateMode = option.storageValue
                                }
                                .padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = autoFrameRateMode == option.storageValue,
                                onClick = {
                                    autoFrameRateMode = option.storageValue
                                    store.autoFrameRateMode = option.storageValue
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Accent)
                            )
                            Column(Modifier.padding(start = 4.dp)) {
                                Text(stringResource(option.labelRes()), color = TextHi, fontSize = 14.sp)
                                Text(stringResource(option.descriptionRes()), color = TextMid, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TvDialogTextButton(label = stringResource(R.string.settings_done), color = AccentSoft, onClick = { dialog = "" })
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
                        Text(stringResource(R.string.settings_tmdb_key_help),
                            color = TextMid, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        SettingField(stringResource(R.string.settings_api_key), key, modifier = Modifier.focusRequester(fT)) { key = it }
                    }
                },
                confirmButton = {
                    TvDialogTextButton(
                        label = stringResource(R.string.settings_save),
                        color = AccentSoft,
                        onClick = {
                            vm.saveTmdbKey(key.trim()); toast(ctx, ctx.getString(R.string.settings_saved)); dialog = ""
                        }
                    )
                },
                dismissButton = { TvDialogTextButton(label = stringResource(R.string.settings_cancel), color = TextMid, onClick = { dialog = "" }) }
            )
        }
        "subs" -> {
            val (k0, u0, p0) = remember { vm.loadSubSettings() }
            var key by remember { mutableStateOf(k0) }
            var user by remember { mutableStateOf(u0) }
            var pass by remember { mutableStateOf(p0) }
            AlertDialog(
                onDismissRequest = { dialog = "" }, containerColor = BgElev2,
                title = { Text(stringResource(R.string.settings_subtitles_dialog_title), color = TextHi) },
                text = {
                    Column {
                        val fS = rememberInitialFocus()
                        Text(stringResource(R.string.settings_subtitles_key_help),
                            color = TextMid, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        SettingField(stringResource(R.string.settings_api_key), key, modifier = Modifier.focusRequester(fS)) { key = it }
                        SettingField(stringResource(R.string.settings_username), user) { user = it }
                        SettingField(stringResource(R.string.settings_password), pass, isPassword = true) { pass = it }
                    }
                },
                confirmButton = {
                    TvDialogTextButton(
                        label = stringResource(R.string.settings_save),
                        color = AccentSoft,
                        onClick = {
                            vm.saveSubSettings(key.trim(), user.trim(), pass.trim())
                            toast(ctx, ctx.getString(R.string.settings_saved)); dialog = ""
                        }
                    )
                },
                dismissButton = { TvDialogTextButton(label = stringResource(R.string.settings_cancel), color = TextMid, onClick = { dialog = "" }) }
            )
        }
    }
}
