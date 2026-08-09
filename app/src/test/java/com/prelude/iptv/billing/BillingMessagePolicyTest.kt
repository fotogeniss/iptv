package com.prelude.iptv.billing

import com.android.billingclient.api.BillingClient
import org.junit.Assert.assertEquals
import org.junit.Test

class BillingMessagePolicyTest {
    @Test
    fun `known billing responses map to typed app messages`() {
        assertEquals(
            BillingMessage.BillingUnavailable,
            BillingMessagePolicy.fromResponse(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE, "detail"),
        )
        assertEquals(
            BillingMessage.ItemAlreadyOwned,
            BillingMessagePolicy.fromResponse(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED, "detail"),
        )
        assertEquals(
            BillingMessage.NetworkUnavailable,
            BillingMessagePolicy.fromResponse(BillingClient.BillingResponseCode.NETWORK_ERROR, "detail"),
        )
    }

    @Test
    fun `unknown Play detail remains provider owned data`() {
        assertEquals(
            BillingMessage.PlayError("Play detail"),
            BillingMessagePolicy.fromResponse(BillingClient.BillingResponseCode.ERROR, "Play detail"),
        )
        assertEquals(
            BillingMessage.PlayError(null),
            BillingMessagePolicy.fromResponse(BillingClient.BillingResponseCode.ERROR, ""),
        )
    }
}
