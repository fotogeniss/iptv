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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileEpgSettingsScreen(
    enabled: Boolean,
    loaded: Boolean,
    sourceType: String,
    currentUrl: String,
    status: String,
    discoveredSources: List<Pair<String, String>>,
    onEnabledChange: (Boolean) -> Unit,
    onDiscover: () -> Unit,
    onUseUrl: (String) -> Unit,
    onCloseDiscovery: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onCloseDiscovery(); onBack() }
    var url by remember { mutableStateOf(currentUrl) }
    var sourcePickerOpen by remember { mutableStateOf(false) }
    var discoverRequested by remember { mutableStateOf(false) }
    LaunchedEffect(currentUrl) { if (url.isBlank()) url = currentUrl }
    LaunchedEffect(discoveredSources, discoverRequested) {
        if (discoverRequested && discoveredSources.isNotEmpty()) sourcePickerOpen = true
    }

    Column(modifier.fillMaxSize().background(Color(0xFF050505))) {
        MobileSettingsFlowHeader("EPG", onBack = { onCloseDiscovery(); onBack() })
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp)) {
            Text("Στοιχεία EPG", color = Color(0xFFD4D4D4), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(13.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF15151B)).padding(17.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(sourceType, color = IptvColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text(if (loaded) "EPG φορτωμένο" else "Δεν έχει φορτωθεί EPG", color = if (loaded) Color(0xFF55C780) else IptvColors.TextTertiary, fontSize = 12.sp)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IptvColors.Primary)
                )
            }

            if (enabled) {
                Spacer(Modifier.height(25.dp))
                Text("XMLTV URL", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(9.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth().height(112.dp),
                    placeholder = { Text("https://provider.example.com/epg.xml", color = IptvColors.TextTertiary, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = IptvColors.Primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                        focusedContainerColor = Color(0xFF111116),
                        unfocusedContainerColor = Color(0xFF111116),
                        cursorColor = IptvColors.Primary
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { discoverRequested = true; onDiscover() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(13.dp)
                ) { Text("Αυτόματη εύρεση EPG", fontWeight = FontWeight.ExtraBold) }
                if (status.isNotBlank()) {
                    Text(status, color = IptvColors.TextSecondary, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp))
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { onUseUrl(url.trim()) },
                    enabled = url.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IptvColors.Primary, disabledContainerColor = Color(0xFF2A2A2A))
                ) { Text("Αποθήκευση και φόρτωση", fontWeight = FontWeight.Black) }
            }
        }
    }

    if (sourcePickerOpen) {
        ModalBottomSheet(
            onDismissRequest = { sourcePickerOpen = false },
            containerColor = Color(0xFF080808),
            contentColor = Color.White
        ) {
            Text("Πηγές EPG για αυτή τη λίστα", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                items(discoveredSources, key = { it.second }) { (label, candidateUrl) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF171717)).clickable {
                                url = candidateUrl
                                sourcePickerOpen = false
                            }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, tint = IptvColors.Primary, modifier = Modifier.size(22.dp))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(maskEpgUrl(candidateUrl), color = IptvColors.TextTertiary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun maskEpgUrl(url: String): String = runCatching {
    val uri = java.net.URI(url)
    val port = uri.port.takeIf { it > 0 }?.let { ":$it" }.orEmpty()
    "${uri.scheme}://${uri.host.orEmpty()}$port/${uri.path.orEmpty().substringAfterLast('/')}"
}.getOrDefault("Αποθηκευμένη πηγή XMLTV")
