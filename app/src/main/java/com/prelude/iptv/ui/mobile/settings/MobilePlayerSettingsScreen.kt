package com.prelude.iptv.ui.mobile.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.prelude.iptv.ui.icons.filled.CheckCircle
import com.prelude.iptv.ui.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.data.PlaybackPreferencePolicy
import com.prelude.iptv.player.BufferPolicy
import com.prelude.iptv.player.BufferProfile
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.localization.labelRes
import com.prelude.iptv.ui.localization.preferenceLanguageLabelRes
import com.prelude.iptv.ui.localization.subtitleSizeLabelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobilePlayerSettingsScreen(
    bufferProfile: String,
    startWithSubtitles: Boolean,
    subtitleSize: Int,
    subtitleLanguage: String,
    audioLanguage: String,
    onBufferProfile: (String) -> Unit,
    onStartWithSubtitles: (Boolean) -> Unit,
    onSubtitleSize: (Int) -> Unit,
    onSubtitleLanguage: (String) -> Unit,
    onAudioLanguage: (String) -> Unit,
    onOpenSubtitlesAccount: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var picker by remember { mutableStateOf<PlayerPicker?>(null) }

    Column(modifier.fillMaxSize().background(IptvColors.Background)) {
        MobileSettingsFlowHeader(stringResource(R.string.settings_player_title), onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)
        ) {
            HorizonGroupLabel(stringResource(R.string.settings_player_general), Modifier.padding(top = 0.dp))
            HorizonSettingsCard {
                HorizonSettingsBlock(stringResource(R.string.settings_player_buffer)) {
                    HorizonSegmented(
                        options = BufferProfile.entries.map { it.storageValue to stringResource(it.labelRes()) },
                        selected = BufferPolicy.fromStorage(bufferProfile).storageValue,
                        onSelect = onBufferProfile,
                    )
                }
            }

            HorizonGroupLabel(stringResource(R.string.settings_player_video))
            HorizonSettingsCard {
                HorizonSettingsRow(
                    title = stringResource(R.string.settings_start_with_subtitles),
                    subtitle = stringResource(
                        if (startWithSubtitles) R.string.settings_start_with_subtitles_on
                        else R.string.settings_start_with_subtitles_off
                    ),
                    onClick = { onStartWithSubtitles(!startWithSubtitles) },
                    trailing = { HorizonSwitch(startWithSubtitles, onStartWithSubtitles) },
                )
                val normalizedSize = PlaybackPreferencePolicy.normalizeSubtitleSize(subtitleSize)
                HorizonSettingsBlock(stringResource(R.string.settings_subtitle_size)) {
                    HorizonSegmented(
                        options = PlaybackPreferencePolicy.subtitleSizes.map { it to stringResource(subtitleSizeLabelRes(it)) },
                        selected = normalizedSize,
                        onSelect = onSubtitleSize,
                    )
                    // Ζωντανή προεπισκόπηση: φαίνεται αμέσως τι αλλάζει.
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.Black)
                            .padding(horizontal = 12.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.settings_subtitle_preview),
                            color = Color.White,
                            fontSize = (15f * normalizedSize / 100f).sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                HorizonSettingsRow(
                    title = stringResource(R.string.settings_subtitle_language),
                    value = stringResource(preferenceLanguageLabelRes(subtitleLanguage)),
                    onClick = { picker = PlayerPicker.SubtitleLanguage },
                )
                HorizonSettingsRow(
                    title = stringResource(R.string.settings_opensubtitles_account),
                    subtitle = stringResource(R.string.settings_opensubtitles_account_subtitle),
                    onClick = onOpenSubtitlesAccount,
                )
            }

            HorizonGroupLabel(stringResource(R.string.settings_player_audio))
            HorizonSettingsCard {
                HorizonSettingsRow(
                    title = stringResource(R.string.settings_default_audio_language),
                    value = stringResource(preferenceLanguageLabelRes(audioLanguage)),
                    onClick = { picker = PlayerPicker.AudioLanguage },
                )
            }
        }
    }

    picker?.let { selectedPicker ->
        val options = pickerOptions(selectedPicker)
        val selectedKey = when (selectedPicker) {
            PlayerPicker.Buffer -> BufferPolicy.fromStorage(bufferProfile).storageValue
            PlayerPicker.SubtitleSize -> PlaybackPreferencePolicy.normalizeSubtitleSize(subtitleSize).toString()
            PlayerPicker.SubtitleLanguage -> PlaybackPreferencePolicy.normalizeLanguage(subtitleLanguage)
            PlayerPicker.AudioLanguage -> PlaybackPreferencePolicy.normalizeLanguage(audioLanguage)
        }
        ModalBottomSheet(
            onDismissRequest = { picker = null },
            containerColor = IptvColors.BackgroundRaised,
            contentColor = Color.White
        ) {
            Text(stringResource(selectedPicker.titleRes()), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                items(options, key = { it.first }) { (key, label) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF171717)).clickable {
                                when (selectedPicker) {
                                    PlayerPicker.Buffer -> onBufferProfile(key)
                                    PlayerPicker.SubtitleSize -> onSubtitleSize(key.toInt())
                                    PlayerPicker.SubtitleLanguage -> onSubtitleLanguage(key)
                                    PlayerPicker.AudioLanguage -> onAudioLanguage(key)
                                }
                                picker = null
                            }.padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Icon(
                            if (key == selectedKey) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            null,
                            tint = if (key == selectedKey) IptvColors.Primary else IptvColors.TextTertiary,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private enum class PlayerPicker { Buffer, SubtitleSize, SubtitleLanguage, AudioLanguage }

private fun PlayerPicker.titleRes(): Int = when (this) {
    PlayerPicker.Buffer -> R.string.settings_player_buffer
    PlayerPicker.SubtitleSize -> R.string.settings_subtitle_size
    PlayerPicker.SubtitleLanguage -> R.string.settings_subtitle_language
    PlayerPicker.AudioLanguage -> R.string.settings_default_audio_language
}

@Composable
private fun pickerOptions(picker: PlayerPicker): List<Pair<String, String>> = when (picker) {
    PlayerPicker.Buffer -> BufferProfile.entries.map { it.storageValue to stringResource(it.labelRes()) }
    PlayerPicker.SubtitleSize -> PlaybackPreferencePolicy.subtitleSizes.map {
        it.toString() to stringResource(subtitleSizeLabelRes(it))
    }
    PlayerPicker.SubtitleLanguage, PlayerPicker.AudioLanguage -> PlaybackPreferencePolicy.languages.map {
        it.code to stringResource(preferenceLanguageLabelRes(it.code))
    }
}
