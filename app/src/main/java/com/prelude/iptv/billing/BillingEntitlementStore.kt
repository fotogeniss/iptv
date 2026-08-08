package com.prelude.iptv.billing

import android.content.Context

/**
 * Offline cache only. A successful Play query always replaces this value,
 * including with FREE when a purchase was revoked or refunded.
 */
internal class BillingEntitlementStore(context: Context) {
    private val preferences = context.getSharedPreferences("billing_entitlement", Context.MODE_PRIVATE)

    fun read(): PremiumEntitlement {
        val state = enumValueOrDefault(
            preferences.getString(KEY_STATE, null),
            PurchaseState.NOT_OWNED,
        )
        val verification = enumValueOrDefault(
            preferences.getString(KEY_VERIFICATION, null),
            VerificationLevel.NONE,
        )
        return PremiumEntitlement(
            state = state,
            productId = preferences.getString(KEY_PRODUCT_ID, null),
            verification = verification,
        )
    }

    fun write(entitlement: PremiumEntitlement) {
        preferences.edit()
            .putString(KEY_STATE, entitlement.state.name)
            .putString(KEY_PRODUCT_ID, entitlement.productId)
            .putString(KEY_VERIFICATION, entitlement.verification.name)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback

    private companion object {
        const val KEY_STATE = "state"
        const val KEY_PRODUCT_ID = "product_id"
        const val KEY_VERIFICATION = "verification"
    }
}
