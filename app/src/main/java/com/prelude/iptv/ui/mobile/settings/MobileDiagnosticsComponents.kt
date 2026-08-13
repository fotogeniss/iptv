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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.diagnostics.DiagnosticsState
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.StreamingRadius
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Suppress("DEPRECATION")
private val acceptedSendIcon: ImageVector = Icons.Default.Send

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
                Text(stringResource(R.string.diagnostics_hero_title), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text(
                    stringResource(
                        if (state.collectionEnabled) {
                            R.string.diagnostics_hero_reporting_enabled
                        } else {
                            R.string.diagnostics_hero_reporting_disabled
                        }
                    ),
                    color = IptvColors.TextSecondary,
                    fontSize = 11.sp,
                )
            }
        }
        Spacer(Modifier.height(17.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DiagnosticsFact(
                stringResource(R.string.diagnostics_fact_analytics),
                stringResource(R.string.diagnostics_fact_none),
                Modifier.weight(1f),
            )
            DiagnosticsFact(
                stringResource(R.string.diagnostics_fact_ad_id),
                stringResource(R.string.diagnostics_fact_not_collected),
                Modifier.weight(1f),
            )
            DiagnosticsFact(
                stringResource(R.string.diagnostics_fact_reports),
                stringResource(
                    if (state.collectionEnabled) {
                        R.string.diagnostics_fact_opt_in
                    } else {
                        R.string.diagnostics_fact_local
                    }
                ),
                Modifier.weight(1f),
            )
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
    val toggleDescription = stringResource(R.string.diagnostics_consent_toggle)
    Row(
        Modifier.fillMaxWidth()
            .background(IptvColors.Surface, RoundedCornerShape(StreamingRadius.Card))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(StreamingRadius.Card))
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.diagnostics_consent_title), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.diagnostics_consent_body),
                color = IptvColors.TextSecondary,
                fontSize = 10.sp,
                lineHeight = 14.sp,
            )
        }
        Spacer(Modifier.size(12.dp))
        Switch(
            checked = state.collectionEnabled,
            onCheckedChange = onEnabledChange,
            modifier = Modifier.semantics {
                contentDescription = toggleDescription
            },
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
            stringResource(R.string.diagnostics_privacy_no_source_title),
            stringResource(R.string.diagnostics_privacy_no_source_body),
        )
        DiagnosticsPrivacyLine(
            Icons.Default.CloudOff,
            stringResource(R.string.diagnostics_privacy_no_analytics_title),
            stringResource(R.string.diagnostics_privacy_no_analytics_body),
        )
        DiagnosticsPrivacyLine(
            Icons.Default.CheckCircle,
            stringResource(R.string.diagnostics_privacy_user_control_title),
            stringResource(R.string.diagnostics_privacy_user_control_body),
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
    val pendingDescription = if (local != null) {
        "${formatDiagnosticTime(local.capturedAtMillis)} · ${local.exceptionType.substringAfterLast('.')}"
    } else {
        stringResource(R.string.diagnostics_pending_firebase_device)
    }
    Column(
        Modifier.fillMaxWidth()
            .background(IptvColors.Surface, RoundedCornerShape(StreamingRadius.Card))
            .border(1.dp, Color(0xFFFF5961).copy(alpha = 0.35f), RoundedCornerShape(StreamingRadius.Card))
            .padding(15.dp)
    ) {
        Text(stringResource(R.string.diagnostics_pending_title), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(5.dp))
        Text(
            pendingDescription,
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
                Icon(acceptedSendIcon, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.diagnostics_send_once), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onDelete,
                enabled = !state.busy,
                modifier = Modifier.weight(0.72f),
            ) {
                Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.size(5.dp))
                Text(stringResource(R.string.diagnostics_delete), fontSize = 10.sp)
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
        Text(stringResource(R.string.diagnostics_setup_title), color = Color(0xFFFFC46B), fontSize = 13.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(5.dp))
        Text(
            stringResource(R.string.diagnostics_setup_body),
            color = IptvColors.TextSecondary,
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun formatDiagnosticTime(timestamp: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    val fallback = stringResource(R.string.diagnostics_previous_run)
    return remember(timestamp, locale) {
        runCatching {
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(locale)
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(timestamp))
        }.getOrNull()
    } ?: fallback
}
