package com.prelude.iptv.ui.mobile.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.diagnostics.DiagnosticsManager
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.mobile.navigation.premiumMobileNavigationContentPadding

@Composable
internal fun MobileDiagnosticsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val state by DiagnosticsManager.state.collectAsState()
    LaunchedEffect(Unit) { DiagnosticsManager.refreshPendingState() }

    Column(modifier.fillMaxSize().background(IptvColors.Background)) {
        MobileSettingsFlowHeader("Διαγνωστικά & crashes", onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = premiumMobileNavigationContentPadding()),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item(key = "diagnostics-hero") { MobileDiagnosticsHero(state) }
            item(key = "diagnostics-consent") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    DiagnosticsConsentCard(state, DiagnosticsManager::setCollectionEnabled)
                }
            }
            item(key = "diagnostics-privacy-title") { DiagnosticsSectionTitle("Τι περιλαμβάνει") }
            item(key = "diagnostics-privacy") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    DiagnosticsPrivacyCard()
                }
            }
            if (state.hasPendingReport) {
                item(key = "diagnostics-pending-title") { DiagnosticsSectionTitle("Εκκρεμές report") }
                item(key = "diagnostics-pending") {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        PendingDiagnosticCard(
                            state = state,
                            onSend = DiagnosticsManager::sendPendingOnce,
                            onDelete = DiagnosticsManager::deletePendingReports,
                        )
                    }
                }
            }
            if (!state.firebaseConfigured) {
                item(key = "diagnostics-setup") {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        DiagnosticsSetupCard()
                    }
                }
            }
            state.message?.let { message ->
                item(key = "diagnostics-message-$message") {
                    Text(
                        message,
                        color = Color(0xFFFF7A80),
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
            }
            item(key = "diagnostics-footer") {
                Text(
                    "Η ενεργοποίηση είναι προαιρετική και μπορεί να ανακληθεί οποτεδήποτε.",
                    color = IptvColors.TextTertiary,
                    fontSize = 9.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DiagnosticsSectionTitle(title: String) {
    Text(
        title,
        color = Color(0xFFC8C8C8),
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(start = 20.dp, top = 7.dp, bottom = 1.dp),
    )
}
