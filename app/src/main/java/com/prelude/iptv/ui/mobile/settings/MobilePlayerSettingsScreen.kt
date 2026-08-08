package com.prelude.iptv.ui.mobile.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.PlaybackPreferencePolicy
import com.prelude.iptv.player.BufferPolicy
import com.prelude.iptv.player.BufferProfile
import com.prelude.iptv.ui.IptvColors

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

    Column(modifier.fillMaxSize().background(Color(0xFF050505))) {
        MobileSettingsFlowHeader("Ρυθμίσεις Player", onBack)
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp)) {
            PlayerGroupTitle("Γενικά")
            PlayerValueRow("Buffer", BufferPolicy.label(BufferPolicy.fromStorage(bufferProfile))) { picker = PlayerPicker.Buffer }
            Spacer(Modifier.height(25.dp))
            PlayerGroupTitle("Βίντεο")
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color(0xFF171717))
                    .clickable { onStartWithSubtitles(!startWithSubtitles) }.padding(horizontal = 17.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Έναρξη με υπότιτλους", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Switch(
                    checked = startWithSubtitles,
                    onCheckedChange = onStartWithSubtitles,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IptvColors.Primary)
                )
            }
            Text(
                if (startWithSubtitles) "Οι υπότιτλοι ανοίγουν αυτόματα όταν υπάρχουν στο περιεχόμενο."
                else "Οι υπότιτλοι παραμένουν κλειστοί μέχρι να τους ενεργοποιήσεις στον player.",
                color = IptvColors.TextTertiary,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp)
            )
            PlayerValueRow("Μέγεθος υποτίτλων", PlaybackPreferencePolicy.subtitleSizeLabel(subtitleSize)) { picker = PlayerPicker.SubtitleSize }
            Spacer(Modifier.height(9.dp))
            PlayerValueRow("Γλώσσα υποτίτλων", PlaybackPreferencePolicy.languageLabel(subtitleLanguage)) { picker = PlayerPicker.SubtitleLanguage }
            Spacer(Modifier.height(9.dp))
            PlayerValueRow("Λογαριασμός OpenSubtitles", "API key & σύνδεση", onOpenSubtitlesAccount)
            Spacer(Modifier.height(25.dp))
            PlayerGroupTitle("Ήχος")
            PlayerValueRow("Προεπιλεγμένη γλώσσα ήχου", PlaybackPreferencePolicy.languageLabel(audioLanguage)) { picker = PlayerPicker.AudioLanguage }
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
            containerColor = Color(0xFF080808),
            contentColor = Color.White
        ) {
            Text(selectedPicker.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
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

@Composable
private fun PlayerGroupTitle(title: String) {
    Text(title, color = Color(0xFFD4D4D4), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 2.dp, vertical = 11.dp))
}

@Composable
private fun PlayerValueRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color(0xFF171717))
            .clickable(onClick = onClick).padding(horizontal = 17.dp, vertical = 21.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(value, color = IptvColors.TextSecondary, fontSize = 11.sp)
    }
}

private enum class PlayerPicker(val title: String) {
    Buffer("Buffer"),
    SubtitleSize("Μέγεθος υποτίτλων"),
    SubtitleLanguage("Γλώσσα υποτίτλων"),
    AudioLanguage("Γλώσσα ήχου")
}

private fun pickerOptions(picker: PlayerPicker): List<Pair<String, String>> = when (picker) {
    PlayerPicker.Buffer -> BufferProfile.entries.map { it.storageValue to BufferPolicy.label(it) }
    PlayerPicker.SubtitleSize -> PlaybackPreferencePolicy.subtitleSizes.map { it.toString() to PlaybackPreferencePolicy.subtitleSizeLabel(it) }
    PlayerPicker.SubtitleLanguage, PlayerPicker.AudioLanguage -> PlaybackPreferencePolicy.languages.map { it.code to it.label }
}
