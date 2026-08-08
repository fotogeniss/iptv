package com.prelude.iptv.billing

/** Products that must also exist, with the same ids, in Google Play Console. */
object BillingProductCatalog {
    const val PRELUDE_PLUS_LIFETIME = "prelude_plus_lifetime"

    val premiumProductIds: Set<String> = setOf(PRELUDE_PLUS_LIFETIME)
}
