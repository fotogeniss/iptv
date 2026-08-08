package com.prelude.iptv.billing

/** Capabilities controlled by the single Premium entitlement. */
enum class PremiumFeature {
    EDIT_HOME,
    SUGGESTIONS,
    PROFILES,
    MULTIVIEW,
    SUBTITLES_ONLINE,
    MULTIPLE_SOURCES,
    BACKUP,
}

enum class PremiumTier {
    FREE,
    PREMIUM,
}

/** The only mapping from entitlement tier to application capabilities. */
object PremiumPolicy {
    val defaultTier: PremiumTier = PremiumTier.FREE

    // Exporting the user's own data must never depend on an active purchase.
    private val freeFeatures: Set<PremiumFeature> = setOf(PremiumFeature.BACKUP)

    fun unlocked(feature: PremiumFeature, tier: PremiumTier): Boolean = when (tier) {
        PremiumTier.PREMIUM -> true
        PremiumTier.FREE -> feature in freeFeatures
    }

    fun tierOf(saved: String): PremiumTier =
        PremiumTier.entries.firstOrNull { it.name == saved } ?: defaultTier
}
