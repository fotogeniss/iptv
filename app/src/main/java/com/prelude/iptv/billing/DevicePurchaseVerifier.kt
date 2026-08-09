package com.prelude.iptv.billing

/**
 * Device-side verifier for evidence delivered by BillingClient.
 * The persisted [VerificationLevel.SERVER] value remains supported for storage
 * compatibility, but this application does not require or promise a backend.
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
            PurchaseVerificationResult.Rejected(INVALID_PURCHASE_EVIDENCE)
        }
    }

    private companion object {
        const val INVALID_PURCHASE_EVIDENCE = "invalid_purchase_evidence"
    }
}
