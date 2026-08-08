package com.prelude.iptv.ui.mobile.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.diagnostics.DiagnosticsState
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.StreamingRadius
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun MobileDiagnosticsHero(state: DiagnosticsState) {
    Column(
        Modifier.fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(IptvColors.Background, Color(0xFF1A0D0E), IptvColors.Background)
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.BugReport,
                contentDescription = null,
                tint = Color(0xFFFF5961),
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.size(11.dp))
            Column {
                Text("Διαγνωστικά εφαρμογής", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text(
                    if (state.collectionEnabled) "Η αναφορά σφαλμάτων είναι ενεργή" else "Ιδιωτικότητα εξ αρχής — απενεργοποιημένο",
                    color = IptvColors.TextSecondary,
                    fontSize = 11.sp,
                )
            }
        }
        Spacer(Modifier.height(17.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DiagnosticsFact("Analytics", "Κανένα", Modifier.weight(1f))
            DiagnosticsFact("Διαφημιστικό ID", "Δεν συλλέγεται", Modifier.weight(1f))
            DiagnosticsFact("Reports", if (state.collectionEnabled) "Opt-in" else "Τοπικά", Modifier.weight(1f))
        }
    }
}

@Composable
private fun DiagnosticsFact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Color(0xFF151515), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(label, color = IptvColors.TextTertiary, fontSize = 8.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
internal fun DiagnosticsConsentCard(
    state: DiagnosticsState,
    onEnabledChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .background(IptvColors.Surface, RoundedCornerShape(StreamingRadius.Card))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(StreamingRadius.Card))
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Crash & ANR reports", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Στείλε τεχνικές πληροφορίες σταθερότητας ώστε να εντοπίζονται πραγματικά προβλήματα.",
                color = IptvColors.TextSecondary,
                fontSize = 10.sp,
                lineHeight = 14.sp,
            )
        }
        Spacer(Modifier.size(12.dp))
        Switch(
            checked = state.collectionEnabled,
            onCheckedChange = onEnabledChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = IptvColors.Primary,
                uncheckedThumbColor = IptvColors.TextSecondary,
                uncheckedTrackColor = IptvColors.SurfaceRaised,
            ),
        )
    }
}

@Composable
internal fun DiagnosticsPrivacyCard() {
    Column(
        Modifier.fillMaxWidth()
            .background(Color(0xFF111111), RoundedCornerShape(StreamingRadius.Card))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(StreamingRadius.Card))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DiagnosticsPrivacyLine(
            Icons.Default.Lock,
            "Χωρίς στοιχεία λίστας",
            "Δεν προσθέτουμε URLs, credentials, τίτλους media ή αναγνωριστικό χρήστη.",
        )
        DiagnosticsPrivacyLine(
            Icons.Default.CloudOff,
            "Χωρίς Analytics",
            "Δεν εγκαθίσταται Firebase Analytics και δεν καταγράφεται συμπεριφορά προβολής.",
        )
        DiagnosticsPrivacyLine(
            Icons.Default.CheckCircle,
            "Έλεγχος από εσένα",
            "Μπορείς να απενεργοποιήσεις τη συλλογή ή να διαγράψεις εκκρεμές report.",
        )
    }
}

@Composable
private fun DiagnosticsPrivacyLine(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Color(0xFFFF5961), modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(10.dp))
        Column {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(body, color = IptvColors.TextSecondary, fontSize = 9.sp, lineHeight = 13.sp)
        }
    }
}

@Composable
internal fun PendingDiagnosticCard(
    state: DiagnosticsState,
    onSend: () -> Unit,
    onDelete: () -> Unit,
) {
    val local = state.pendingLocalReport
    Column(
        Modifier.fillMaxWidth()
            .background(IptvColors.Surface, RoundedCornerShape(StreamingRadius.Card))
            .border(1.dp, Color(0xFFFF5961).copy(alpha = 0.35f), RoundedCornerShape(StreamingRadius.Card))
            .padding(15.dp)
    ) {
        Text("Υπάρχει εκκρεμές report", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(5.dp))
        Text(
            local?.let { "${formatDiagnosticTime(it.capturedAtMillis)} · ${it.exceptionType.substringAfterLast('.')}" }
                ?: "Αποθηκευμένο από το Firebase στη συσκευή",
            color = IptvColors.TextSecondary,
            fontSize = 10.sp,
        )
        local?.summary?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(5.dp))
            Text(it, color = IptvColors.TextTertiary, fontSize = 9.sp, maxLines = 2)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Button(
                onClick = onSend,
                enabled = !state.busy,
                colors = ButtonDefaults.buttonColors(containerColor = IptvColors.Primary),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Send, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.size(6.dp))
                Text("Αποστολή μία φορά", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onDelete,
                enabled = !state.busy,
                modifier = Modifier.weight(0.72f),
            ) {
                Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.size(5.dp))
                Text("Διαγραφή", fontSize = 10.sp)
            }
        }
    }
}

@Composable
internal fun DiagnosticsSetupCard() {
    Column(
        Modifier.fillMaxWidth()
            .background(Color(0xFF17120C), RoundedCornerShape(StreamingRadius.Card))
            .border(1.dp, Color(0xFFFFB74D).copy(alpha = 0.35f), RoundedCornerShape(StreamingRadius.Card))
            .padding(15.dp)
    ) {
        Text("Απομένει σύνδεση Firebase", color = Color(0xFFFFC46B), fontSize = 13.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(5.dp))
        Text(
            "Η τοπική καταγραφή λειτουργεί. Για να βλέπει ο ιδιοκτήτης τα reports στο Firebase Console, χρειάζεται το app/google-services.json του δικού του project.",
            color = IptvColors.TextSecondary,
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
    }
}

private fun formatDiagnosticTime(timestamp: Long): String = runCatching {
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(timestamp))
}.getOrDefault("Προηγούμενη εκτέλεση")
