package com.prelude.iptv.billing

/** Pure entitlement reduction, kept independent from Android and BillingClient. */
object BillingEntitlementPolicy {
    fun resolve(
        purchases: List<PurchaseSnapshot>,
        premiumProductIds: Set<String> = BillingProductCatalog.premiumProductIds,
    ): PremiumEntitlement {
        val relevant = purchases.filter { purchase ->
            purchase.productIds.any(premiumProductIds::contains)
        }

        val active = relevant.firstOrNull {
            it.state == PurchaseState.PURCHASED && it.verification != VerificationLevel.NONE
        }
        if (active != null) {
            return PremiumEntitlement(
                state = PurchaseState.PURCHASED,
                productId = active.productIds.firstOrNull(premiumProductIds::contains),
                verification = active.verification,
            )
        }

        val pending = relevant.firstOrNull { it.state == PurchaseState.PENDING }
        if (pending != null) {
            return PremiumEntitlement(
                state = PurchaseState.PENDING,
                productId = pending.productIds.firstOrNull(premiumProductIds::contains),
            )
        }

        return PremiumEntitlement()
    }
}
