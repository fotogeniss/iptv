package com.prelude.iptv.billing

import com.android.billingclient.api.BillingClient

internal object BillingMessagePolicy {
    fun fromResponse(responseCode: Int, debugMessage: String): BillingMessage = when (responseCode) {
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> BillingMessage.BillingUnavailable
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> BillingMessage.ItemUnavailable
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> BillingMessage.ItemAlreadyOwned
        BillingClient.BillingResponseCode.NETWORK_ERROR,
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> BillingMessage.NetworkUnavailable
        else -> BillingMessage.PlayError(debugMessage.takeIf(String::isNotBlank))
    }
}
