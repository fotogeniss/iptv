package com.prelude.iptv.billing

import org.junit.Assert.assertEquals
import org.junit.Test

class BillingEntitlementPolicyTest {
    private val product = BillingProductCatalog.PRELUDE_PLUS_LIFETIME

    @Test
    fun `pending purchase never unlocks premium`() {
        val result = BillingEntitlementPolicy.resolve(
            listOf(snapshot(PurchaseState.PENDING, VerificationLevel.NONE))
        )

        assertEquals(PurchaseState.PENDING, result.state)
        assertEquals(PremiumTier.FREE, result.tier)
    }

    @Test
    fun `unverified purchased state never unlocks premium`() {
        val result = BillingEntitlementPolicy.resolve(
            listOf(snapshot(PurchaseState.PURCHASED, VerificationLevel.NONE))
        )

        assertEquals(PremiumTier.FREE, result.tier)
    }

    @Test
    fun `device verified Play purchase unlocks premium`() {
        val result = BillingEntitlementPolicy.resolve(
            listOf(snapshot(PurchaseState.PURCHASED, VerificationLevel.PLAY_DEVICE))
        )

        assertEquals(product, result.productId)
        assertEquals(PremiumTier.PREMIUM, result.tier)
    }

    @Test
    fun `server verified Play purchase unlocks premium`() {
        val result = BillingEntitlementPolicy.resolve(
            listOf(snapshot(PurchaseState.PURCHASED, VerificationLevel.SERVER))
        )

        assertEquals(PremiumTier.PREMIUM, result.tier)
    }

    @Test
    fun `missing purchase revokes cached entitlement`() {
        val result = BillingEntitlementPolicy.resolve(emptyList())

        assertEquals(PremiumEntitlement(), result)
    }

    @Test
    fun `unrelated product cannot unlock premium`() {
        val result = BillingEntitlementPolicy.resolve(
            listOf(
                PurchaseSnapshot(
                    productIds = setOf("different_product"),
                    state = PurchaseState.PURCHASED,
                    purchaseToken = "token",
                    acknowledged = true,
                    verification = VerificationLevel.SERVER,
                )
            )
        )

        assertEquals(PremiumTier.FREE, result.tier)
    }

    private fun snapshot(state: PurchaseState, verification: VerificationLevel) = PurchaseSnapshot(
        productIds = setOf(product),
        state = state,
        purchaseToken = "token",
        acknowledged = false,
        verification = verification,
    )
}
