package com.prelude.iptv.ui.mobile.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.mobile.navigation.premiumMobileNavigationContentPadding

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MobileLegalPrivacyScreen(
    version: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    var selectedTab by rememberSaveable { mutableStateOf(MobileLegalTab.PRIVACY) }
    var expandedDisclosures by remember { mutableStateOf<Set<String>>(emptySet()) }

    Column(modifier.fillMaxSize().background(IptvColors.Background)) {
        MobileSettingsFlowHeader("Νομικά & απόρρητο", onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = premiumMobileNavigationContentPadding()),
        ) {
            item(key = "legal-hero") {
                MobileLegalHero()
            }

            stickyHeader(key = "legal-tabs") {
                MobileLegalTabs(selected = selectedTab, onSelected = { selectedTab = it })
            }

            item(key = "legal-content-$selectedTab") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    when (selectedTab) {
                        MobileLegalTab.PRIVACY -> MobilePrivacyTab(
                            expanded = expandedDisclosures,
                            onToggle = { id ->
                                expandedDisclosures = if (id in expandedDisclosures) {
                                    expandedDisclosures - id
                                } else {
                                    expandedDisclosures + id
                                }
                            },
                        )

                        MobileLegalTab.TERMS -> MobileTermsTab()
                        MobileLegalTab.SERVICES -> MobileServicesTab()
                    }

                    if (!MobileLegalContent.identityConfigured) {
                        MobileLegalNotice(
                            title = "Χρειάζεται πριν τη δημοσίευση",
                            body = "Δεν έχουν οριστεί ακόμη το νόμιμο όνομα εκδότη και το email απορρήτου. Θα προστεθούν πριν από δημόσια έκδοση — δεν χρησιμοποιούνται ψεύτικα ή προσωρινά στοιχεία.",
                            warning = true,
                        )
                    }

                    MobileLegalFooter(version)
                }
            }
        }
    }
}

@Composable
private fun MobilePrivacyTab(expanded: Set<String>, onToggle: (String) -> Unit) {
    MobileLegalSectionTitle("Τι αποθηκεύεται στη συσκευή")
    MobileLegalDisclosureCard(
        disclosures = MobileLegalContent.localDisclosures,
        expanded = expanded,
        onToggle = onToggle,
    )
    MobileLegalSectionTitle("Τι αποστέλλεται εκτός συσκευής")
    MobileLegalDisclosureCard(
        disclosures = MobileLegalContent.networkDisclosures,
        expanded = expanded,
        onToggle = onToggle,
    )
    MobileLegalNotice(
        title = "Σημαντικό για HTTP λίστες",
        body = "Ορισμένοι IPTV providers λειτουργούν μόνο με μη κρυπτογραφημένο HTTP. Σε αυτή την περίπτωση το δίκτυο μπορεί να δει τη διεύθυνση και την κίνηση προς τον πάροχο. Προτίμησε HTTPS όπου υποστηρίζεται.",
    )
}

@Composable
private fun MobileTermsTab() {
    MobileLegalSectionTitle("Όροι χρήσης")
    MobileLegalTermsCard(MobileLegalContent.terms)
}

@Composable
private fun MobileServicesTab() {
    MobileLegalSectionTitle("Εξωτερικές υπηρεσίες")
    MobileLegalServicesCard(MobileLegalContent.services)
    MobileLegalNotice(
        title = "Αναφορά TMDB",
        body = MobileLegalContent.TMDB_ATTRIBUTION,
    )
}

@Composable
private fun MobileLegalFooter(version: String) {
    Spacer(Modifier.height(24.dp))
    Text(
        "Ισχύς: ${MobileLegalContent.EFFECTIVE_DATE}  ·  Πολιτική ${MobileLegalContent.POLICY_VERSION}",
        color = IptvColors.TextTertiary,
        fontSize = 9.sp,
        lineHeight = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        if (MobileLegalContent.identityConfigured) {
            "${MobileLegalContent.PUBLISHER_LEGAL_NAME}  ·  ${MobileLegalContent.PRIVACY_EMAIL}"
        } else {
            "Στοιχεία επικοινωνίας απορρήτου: εκκρεμούν για release"
        },
        color = if (MobileLegalContent.identityConfigured) Color(0xFFFF5961) else IptvColors.Warning,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
    Text(
        "PRELUDE+ ${version.ifBlank { "—" }}",
        color = IptvColors.TextTertiary,
        fontSize = 8.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
    )
    Spacer(Modifier.height(18.dp))
}
