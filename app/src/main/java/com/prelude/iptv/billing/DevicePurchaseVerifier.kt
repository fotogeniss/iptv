package com.prelude.iptv.billing

/**
 * Temporary client-side verifier used until the account backend is connected.
 * It accepts only evidence delivered by BillingClient for our known product.
 * Production server verification will return [VerificationLevel.SERVER].
 */
internal class DevicePurchaseVerifier(
    private val expectedPackageName: String,
) : PurchaseVerifier {
    override suspend fun verify(evidence: PurchaseEvidence): PurchaseVerificationResult {
        val knownProduct = evidence.productIds.any(BillingProductCatalog.premiumProductIds::contains)
        return if (
            evidence.packageName == expectedPackageName &&
            knownProduct &&
            evidence.purchaseToken.isNotBlank() &&
            evidence.originalJson.isNotBlank()
        ) {
            PurchaseVerificationResult.Accepted(VerificationLevel.PLAY_DEVICE)
        } else {
            PurchaseVerificationResult.Rejected("Η αγορά δεν αντιστοιχεί σε προϊόν του PRELUDE+.")
        }
    }
}
