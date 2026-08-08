package com.prelude.iptv.billing

enum class BillingConnectionState {
    DISCONNECTED,
    CONNECTING,
    READY,
    UNAVAILABLE,
}

enum class PurchaseState {
    NOT_OWNED,
    PENDING,
    PURCHASED,
}

enum class VerificationLevel {
    NONE,
    PLAY_DEVICE,
    SERVER,
}

data class PremiumEntitlement(
    val state: PurchaseState = PurchaseState.NOT_OWNED,
    val productId: String? = null,
    val verification: VerificationLevel = VerificationLevel.NONE,
) {
    val tier: PremiumTier
        get() = if (state == PurchaseState.PURCHASED && verification != VerificationLevel.NONE) {
            PremiumTier.PREMIUM
        } else {
            PremiumTier.FREE
        }
}

data class BillingOffer(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
)

data class BillingUiState(
    val connection: BillingConnectionState = BillingConnectionState.DISCONNECTED,
    val entitlement: PremiumEntitlement = PremiumEntitlement(),
    val offer: BillingOffer? = null,
    val working: Boolean = false,
    val message: String? = null,
)

data class PurchaseSnapshot(
    val productIds: Set<String>,
    val state: PurchaseState,
    val purchaseToken: String,
    val acknowledged: Boolean,
    val verification: VerificationLevel,
)

data class PurchaseEvidence(
    val packageName: String,
    val productIds: Set<String>,
    val purchaseToken: String,
    val originalJson: String,
    val signature: String,
)

sealed interface PurchaseVerificationResult {
    data class Accepted(val level: VerificationLevel) : PurchaseVerificationResult
    data class Rejected(val reason: String) : PurchaseVerificationResult
}

/**
 * Boundary for server-side Play Developer API verification.
 *
 * The device implementation is intentionally replaceable. The backend phase
 * plugs into this interface without changing BillingClient or the UI.
 */
fun interface PurchaseVerifier {
    suspend fun verify(evidence: PurchaseEvidence): PurchaseVerificationResult
}
