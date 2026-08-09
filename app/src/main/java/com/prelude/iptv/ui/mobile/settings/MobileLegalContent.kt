package com.prelude.iptv.ui.mobile.settings

internal enum class MobileLegalTab {
    PRIVACY,
    TERMS,
    SERVICES,
}

internal enum class MobileLegalDisclosure(
    val id: String,
    val icon: MobileLegalIcon,
) {
    SOURCES("sources", MobileLegalIcon.STORAGE),
    PREFERENCES("preferences", MobileLegalIcon.FAVORITES),
    CACHE("cache", MobileLegalIcon.CACHE),
    NETWORK("network", MobileLegalIcon.NETWORK),
    DIAGNOSTICS("diagnostics", MobileLegalIcon.NETWORK),
}

internal enum class MobileLegalService(
    val id: String,
    val badge: String,
) {
    IPTV("iptv", "IPTV"),
    TMDB("tmdb", "TMDB"),
    OPENSUBTITLES("opensubtitles", "OS"),
    GOOGLE_PLAY("google_play", "PLAY"),
    FIREBASE_CRASHLYTICS("firebase_crashlytics", "CRASH"),
}

internal enum class MobileLegalTerm {
    PLAYER_ONLY,
    USER_SOURCES,
    CREDENTIAL_SECURITY,
    THIRD_PARTY_SERVICES,
    PREMIUM_PURCHASES,
    AVAILABILITY,
    CHANGES,
}

internal enum class MobileLegalIcon {
    STORAGE,
    FAVORITES,
    CACHE,
    NETWORK,
}

/**
 * Typed catalog for the approved mobile legal presentation.
 *
 * Display copy is resolved from paired resources at the UI boundary. The
 * publishable, long-form sources remain in docs/PRIVACY_POLICY.md and
 * docs/TERMS_OF_USE.md. Any behavior change must update both places before a
 * store release.
 */
internal object MobileLegalContent {
    const val POLICY_VERSION = "1.1-draft"
    const val EFFECTIVE_DATE = "2026-08-02"
    const val TMDB_ATTRIBUTION = "This product uses the TMDB API but is not endorsed or certified by TMDB."

    // Must be replaced with the entity and address used by the Play listing.
    const val PUBLISHER_LEGAL_NAME = ""
    const val PRIVACY_EMAIL = ""

    val identityConfigured: Boolean
        get() = PUBLISHER_LEGAL_NAME.isNotBlank() && PRIVACY_EMAIL.isNotBlank()

    val localDisclosures = listOf(
        MobileLegalDisclosure.SOURCES,
        MobileLegalDisclosure.PREFERENCES,
        MobileLegalDisclosure.CACHE,
    )

    val networkDisclosures = listOf(
        MobileLegalDisclosure.NETWORK,
        MobileLegalDisclosure.DIAGNOSTICS,
    )

    val terms = listOf(
        MobileLegalTerm.PLAYER_ONLY,
        MobileLegalTerm.USER_SOURCES,
        MobileLegalTerm.CREDENTIAL_SECURITY,
        MobileLegalTerm.THIRD_PARTY_SERVICES,
        MobileLegalTerm.PREMIUM_PURCHASES,
        MobileLegalTerm.AVAILABILITY,
        MobileLegalTerm.CHANGES,
    )

    val services = listOf(
        MobileLegalService.IPTV,
        MobileLegalService.TMDB,
        MobileLegalService.OPENSUBTITLES,
        MobileLegalService.GOOGLE_PLAY,
        MobileLegalService.FIREBASE_CRASHLYTICS,
    )
}
