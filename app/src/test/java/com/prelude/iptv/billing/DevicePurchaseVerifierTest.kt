package com.prelude.iptv.billing

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePurchaseVerifierTest {
    private val verifier = DevicePurchaseVerifier("com.prelude.iptv")

    @Test
    fun `accepts only known product from the expected package`() = runTest {
        val result = verifier.verify(evidence())

        assertTrue(result is PurchaseVerificationResult.Accepted)
    }

    @Test
    fun `rejects a purchase reported for another package`() = runTest {
        val result = verifier.verify(evidence(packageName = "other.package"))

        assertTrue(result is PurchaseVerificationResult.Rejected)
    }

    @Test
    fun `rejects an unrelated product`() = runTest {
        val result = verifier.verify(evidence(productIds = setOf("other_product")))

        assertTrue(result is PurchaseVerificationResult.Rejected)
    }

    private fun evidence(
        packageName: String = "com.prelude.iptv",
        productIds: Set<String> = BillingProductCatalog.premiumProductIds,
    ) = PurchaseEvidence(
        packageName = packageName,
        productIds = productIds,
        purchaseToken = "token",
        originalJson = "{}",
        signature = "signature",
    )
}
