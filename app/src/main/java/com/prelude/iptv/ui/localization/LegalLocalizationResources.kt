package com.prelude.iptv.ui.localization

import androidx.annotation.StringRes
import com.prelude.iptv.R
import com.prelude.iptv.ui.mobile.settings.MobileLegalDisclosure
import com.prelude.iptv.ui.mobile.settings.MobileLegalService
import com.prelude.iptv.ui.mobile.settings.MobileLegalTab
import com.prelude.iptv.ui.mobile.settings.MobileLegalTerm

internal data class LegalDisclosureResources(
    @StringRes val title: Int,
    @StringRes val summary: Int,
    @StringRes val details: Int,
)

internal data class LegalServiceResources(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val status: Int,
)

internal data class LegalTermResources(
    @StringRes val title: Int,
    @StringRes val body: Int,
)

@StringRes
internal fun MobileLegalTab.labelRes(): Int = when (this) {
    MobileLegalTab.PRIVACY -> R.string.legal_tab_privacy
    MobileLegalTab.TERMS -> R.string.legal_tab_terms
    MobileLegalTab.SERVICES -> R.string.legal_tab_services
}

internal fun MobileLegalDisclosure.resources(): LegalDisclosureResources = when (this) {
    MobileLegalDisclosure.SOURCES -> LegalDisclosureResources(
        R.string.legal_disclosure_sources_title,
        R.string.legal_disclosure_sources_summary,
        R.string.legal_disclosure_sources_details,
    )
    MobileLegalDisclosure.PREFERENCES -> LegalDisclosureResources(
        R.string.legal_disclosure_preferences_title,
        R.string.legal_disclosure_preferences_summary,
        R.string.legal_disclosure_preferences_details,
    )
    MobileLegalDisclosure.CACHE -> LegalDisclosureResources(
        R.string.legal_disclosure_cache_title,
        R.string.legal_disclosure_cache_summary,
        R.string.legal_disclosure_cache_details,
    )
    MobileLegalDisclosure.NETWORK -> LegalDisclosureResources(
        R.string.legal_disclosure_network_title,
        R.string.legal_disclosure_network_summary,
        R.string.legal_disclosure_network_details,
    )
    MobileLegalDisclosure.DIAGNOSTICS -> LegalDisclosureResources(
        R.string.legal_disclosure_diagnostics_title,
        R.string.legal_disclosure_diagnostics_summary,
        R.string.legal_disclosure_diagnostics_details,
    )
}

internal fun MobileLegalService.resources(): LegalServiceResources = when (this) {
    MobileLegalService.IPTV -> LegalServiceResources(
        R.string.legal_service_iptv_title,
        R.string.legal_service_iptv_description,
        R.string.legal_service_iptv_status,
    )
    MobileLegalService.TMDB -> LegalServiceResources(
        R.string.legal_service_tmdb_title,
        R.string.legal_service_tmdb_description,
        R.string.legal_service_tmdb_status,
    )
    MobileLegalService.OPENSUBTITLES -> LegalServiceResources(
        R.string.legal_service_opensubtitles_title,
        R.string.legal_service_opensubtitles_description,
        R.string.legal_service_opensubtitles_status,
    )
    MobileLegalService.GOOGLE_PLAY -> LegalServiceResources(
        R.string.legal_service_google_play_title,
        R.string.legal_service_google_play_description,
        R.string.legal_service_google_play_status,
    )
    MobileLegalService.FIREBASE_CRASHLYTICS -> LegalServiceResources(
        R.string.legal_service_crashlytics_title,
        R.string.legal_service_crashlytics_description,
        R.string.legal_service_crashlytics_status,
    )
}

internal fun MobileLegalTerm.resources(): LegalTermResources = when (this) {
    MobileLegalTerm.PLAYER_ONLY -> LegalTermResources(
        R.string.legal_term_player_only_title,
        R.string.legal_term_player_only_body,
    )
    MobileLegalTerm.USER_SOURCES -> LegalTermResources(
        R.string.legal_term_user_sources_title,
        R.string.legal_term_user_sources_body,
    )
    MobileLegalTerm.CREDENTIAL_SECURITY -> LegalTermResources(
        R.string.legal_term_credential_security_title,
        R.string.legal_term_credential_security_body,
    )
    MobileLegalTerm.THIRD_PARTY_SERVICES -> LegalTermResources(
        R.string.legal_term_third_party_services_title,
        R.string.legal_term_third_party_services_body,
    )
    MobileLegalTerm.PREMIUM_PURCHASES -> LegalTermResources(
        R.string.legal_term_premium_purchases_title,
        R.string.legal_term_premium_purchases_body,
    )
    MobileLegalTerm.AVAILABILITY -> LegalTermResources(
        R.string.legal_term_availability_title,
        R.string.legal_term_availability_body,
    )
    MobileLegalTerm.CHANGES -> LegalTermResources(
        R.string.legal_term_changes_title,
        R.string.legal_term_changes_body,
    )
}
