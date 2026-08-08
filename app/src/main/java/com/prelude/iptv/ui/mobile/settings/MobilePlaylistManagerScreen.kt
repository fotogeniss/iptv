package com.prelude.iptv.ui.mobile.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.PlaylistPreferencePolicy
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.settings.SettingsSourceUi

@Composable
internal fun MobilePlaylistManagerScreen(
    source: SettingsSourceUi,
    autoOpen: Boolean,
    refreshDays: Int,
    onAutoOpenChange: (Boolean) -> Unit,
    onRefreshDaysChange: (Int) -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRefresh: () -> Unit,
    onManageAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    Column(modifier.fillMaxSize().background(Color(0xFF050505))) {
        MobileSettingsFlowHeader("Η λίστα μου", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 24.dp)) {
            Text("Ενεργή λίστα", color = Color(0xFFD4D4D4), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF131313))
                    .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(18.dp)).padding(19.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(source.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    SourceBadge(source.typeLabel)
                }
                Spacer(Modifier.height(20.dp))
                SourceFact("Κατάσταση", source.statusLabel)
                SourceFact("Τύπος", source.typeLabel)
                SourceFact("Διεύθυνση", source.endpoint)
                SourceFact("Φορτωμένα στοιχεία", source.channelCount?.toString() ?: "—")
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)); Text("  Επεξεργασία")
                }
                Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = IptvColors.Primary), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(18.dp)); Text("  Διαγραφή")
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(13.dp)
            ) { Icon(Icons.Default.PlayArrow, null); Text("  Άνοιγμα λίστας", fontWeight = FontWeight.Black) }
            Spacer(Modifier.height(9.dp))
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(13.dp)) {
                Icon(Icons.Default.Refresh, null); Text("  Ανανέωση περιεχομένου", fontWeight = FontWeight.ExtraBold)
            }
            TextButton(onClick = onManageAll, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Όλες οι αποθηκευμένες πηγές", color = IptvColors.TextSecondary, fontSize = 11.sp)
            }
            Spacer(Modifier.height(27.dp))
            Text("Προτιμήσεις", color = Color(0xFFD4D4D4), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color(0xFF171717))
                    .clickable { onAutoOpenChange(!autoOpen) }.padding(horizontal = 17.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Αυτόματο άνοιγμα", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(if (autoOpen) "Ενεργό" else "Ανενεργό", color = IptvColors.TextTertiary, fontSize = 10.sp)
                }
                Switch(
                    checked = autoOpen,
                    onCheckedChange = onAutoOpenChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IptvColors.Primary)
                )
            }
            Text(
                if (autoOpen) "Η τελευταία ενεργή λίστα ανοίγει αυτόματα κατά την εκκίνηση."
                else "Θα επιλέγεις χειροκίνητα τη λίστα μετά την εκκίνηση.",
                color = IptvColors.TextTertiary, fontSize = 11.sp, lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp)
            )
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color(0xFF171717))
                    .clickable { onRefreshDaysChange(PlaylistPreferencePolicy.nextRefreshDays(refreshDays)) }.padding(17.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Συχνότητα ενημέρωσης", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(PlaylistPreferencePolicy.refreshDaysLabel(refreshDays), color = IptvColors.TextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(38.dp))
        }
    }
}

@Composable
private fun SourceBadge(label: String) {
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF292929)).padding(horizontal = 9.dp, vertical = 5.dp)) {
        Text(label, color = Color(0xFFB0B0B0), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SourceFact(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text("$label:", color = Color(0xFF888888), fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
