package com.prelude.iptv.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayBillingRepository internal constructor(
    context: Context,
    private val verifier: PurchaseVerifier = DevicePurchaseVerifier(context.packageName),
) : PurchasesUpdatedListener {
    private val entitlementStore = BillingEntitlementStore(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(
        BillingUiState(entitlement = entitlementStore.read())
    )
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    private var connecting = false
    private var restoreRequested = false
    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        if (billingClient.isReady) {
            refreshPurchases(userInitiated = false)
            return
        }
        if (connecting) return
        connecting = true
        _state.update { it.copy(connection = BillingConnectionState.CONNECTING) }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.update {
                        it.copy(connection = BillingConnectionState.READY, message = null)
                    }
                    queryOffer()
                    val restoring = restoreRequested
                    restoreRequested = false
                    refreshPurchases(userInitiated = restoring)
                } else {
                    publishBillingError(result, working = false)
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
                _state.update { it.copy(connection = BillingConnectionState.DISCONNECTED) }
            }
        })
    }

    fun onAppResumed() {
        start()
    }

    fun restorePurchases() {
        restoreRequested = true
        _state.update { it.copy(working = true, message = "Έλεγχος αγορών στο Google Play…") }
        if (billingClient.isReady) {
            restoreRequested = false
            refreshPurchases(userInitiated = true)
        } else {
            start()
        }
    }

    fun launchPremiumPurchase(activity: Activity) {
        if (_state.value.entitlement.tier == PremiumTier.PREMIUM) {
            _state.update { it.copy(message = "Το PRELUDE+ Premium είναι ήδη ενεργό.") }
            return
        }
        if (!billingClient.isReady) {
            _state.update { it.copy(message = "Σύνδεση με το Google Play…") }
            start()
            return
        }

        _state.update { it.copy(working = true, message = null) }
        queryPremiumProduct { details, result ->
            if (details == null) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.update {
                        it.copy(working = false, message = "Το προϊόν Premium δεν έχει ρυθμιστεί ακόμη στο Google Play.")
                    }
                } else {
                    publishBillingError(result, working = false)
                }
                return@queryPremiumProduct
            }

            val offer = preferredOneTimeOffer(details)
            if (offer == null) {
                _state.update {
                    it.copy(working = false, message = "Το προϊόν δεν είναι διαθέσιμο για αυτόν τον λογαριασμό.")
                }
                return@queryPremiumProduct
            }

            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .apply {
                    offer.offerToken
                        ?.takeIf { it.isNotBlank() }
                        ?.let { token -> setOfferToken(token) }
                }
                .build()
            val flowResult = billingClient.launchBillingFlow(
                activity,
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productParams))
                    .build()
            )
            if (flowResult.responseCode != BillingClient.BillingResponseCode.OK) {
                publishBillingError(flowResult, working = false)
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> processPurchases(purchases.orEmpty(), restored = false)
            BillingClient.BillingResponseCode.USER_CANCELED ->
                _state.update { it.copy(working = false, message = "Η αγορά ακυρώθηκε.") }
            else -> publishBillingError(result, working = false)
        }
    }

    private fun queryOffer() {
        queryPremiumProduct { details, result ->
            if (details == null) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    publishBillingError(result, working = false)
                }
                return@queryPremiumProduct
            }
            val offer = preferredOneTimeOffer(details) ?: return@queryPremiumProduct
            _state.update {
                it.copy(
                    offer = BillingOffer(
                        productId = details.productId,
                        title = details.name,
                        description = details.description,
                        formattedPrice = offer.formattedPrice,
                    )
                )
            }
        }
    }

    private fun queryPremiumProduct(callback: (ProductDetails?, BillingResult) -> Unit) {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(BillingProductCatalog.PRELUDE_PLUS_LIFETIME)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()
        billingClient.queryProductDetailsAsync(params) { result, detailsResult ->
            callback(detailsResult.productDetailsList.firstOrNull(), result)
        }
    }

    private fun preferredOneTimeOffer(details: ProductDetails): ProductDetails.OneTimePurchaseOfferDetails? =
        details.oneTimePurchaseOfferDetailsList?.firstOrNull()
            ?: details.oneTimePurchaseOfferDetails

    private fun refreshPurchases(userInitiated: Boolean) {
        if (!billingClient.isReady) {
            if (userInitiated) start()
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases, restored = userInitiated)
            } else {
                // A network/Play failure must not revoke a cached entitlement.
                publishBillingError(result, working = false)
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>, restored: Boolean) {
        scope.launch {
            val snapshots = purchases.mapNotNull { purchase -> snapshotOf(purchase) }
            val entitlement = BillingEntitlementPolicy.resolve(snapshots)
            entitlementStore.write(entitlement)

            val pending = entitlement.state == PurchaseState.PENDING
            _state.update {
                it.copy(
                    entitlement = entitlement,
                    working = false,
                    message = when {
                        pending -> "Η αγορά εκκρεμεί. Το Premium θα ενεργοποιηθεί μόλις ολοκληρωθεί η πληρωμή."
                        restored && entitlement.tier == PremiumTier.PREMIUM -> "Η αγορά επαναφέρθηκε."
                        restored -> "Δεν βρέθηκε ενεργή αγορά για αυτόν τον λογαριασμό Google Play."
                        entitlement.tier == PremiumTier.PREMIUM -> "Το PRELUDE+ Premium ενεργοποιήθηκε."
                        else -> null
                    },
                )
            }
        }
    }

    private suspend fun snapshotOf(purchase: Purchase): PurchaseSnapshot? {
        val state = when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> PurchaseState.PURCHASED
            Purchase.PurchaseState.PENDING -> PurchaseState.PENDING
            else -> PurchaseState.NOT_OWNED
        }
        if (state != PurchaseState.PURCHASED) {
            return PurchaseSnapshot(
                productIds = purchase.products.toSet(),
                state = state,
                purchaseToken = purchase.purchaseToken,
                acknowledged = purchase.isAcknowledged,
                verification = VerificationLevel.NONE,
            )
        }

        val verification = verifier.verify(
            PurchaseEvidence(
                packageName = purchase.packageName,
                productIds = purchase.products.toSet(),
                purchaseToken = purchase.purchaseToken,
                originalJson = purchase.originalJson,
                signature = purchase.signature,
            )
        )
        val accepted = verification as? PurchaseVerificationResult.Accepted
            ?: return null

        if (!purchase.isAcknowledged) acknowledge(purchase.purchaseToken)
        return PurchaseSnapshot(
            productIds = purchase.products.toSet(),
            state = state,
            purchaseToken = purchase.purchaseToken,
            acknowledged = purchase.isAcknowledged,
            verification = accepted.level,
        )
    }

    private fun acknowledge(purchaseToken: String) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.update {
                    it.copy(message = "Η αγορά ενεργοποιήθηκε, αλλά η επιβεβαίωση στο Google Play θα επαναληφθεί.")
                }
            }
        }
    }

    private fun publishBillingError(result: BillingResult, working: Boolean) {
        _state.update {
            it.copy(
                connection = if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    it.connection
                } else {
                    BillingConnectionState.UNAVAILABLE
                },
                working = working,
                message = billingMessage(result),
            )
        }
    }

    private fun billingMessage(result: BillingResult): String = when (result.responseCode) {
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
            "Το Google Play Billing δεν είναι διαθέσιμο σε αυτή τη συσκευή ή στον λογαριασμό."
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
            "Το προϊόν Premium δεν είναι ακόμη διαθέσιμο στο Google Play."
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
            "Η αγορά υπάρχει ήδη. Πάτησε επαναφορά αγορών."
        BillingClient.BillingResponseCode.NETWORK_ERROR,
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ->
            "Δεν ήταν δυνατή η επικοινωνία με το Google Play. Δοκίμασε ξανά."
        else -> result.debugMessage.takeIf(String::isNotBlank)
            ?: "Παρουσιάστηκε πρόβλημα με το Google Play Billing."
    }
}
