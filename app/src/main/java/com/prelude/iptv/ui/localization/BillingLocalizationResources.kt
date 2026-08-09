package com.prelude.iptv.ui.localization

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.prelude.iptv.R
import com.prelude.iptv.billing.BillingMessage
import com.prelude.iptv.billing.PremiumFeature

@Composable
fun BillingMessage.localizedText(): String = when (this) {
    BillingMessage.CheckingPurchases -> stringResource(R.string.billing_message_checking_purchases)
    BillingMessage.PremiumAlreadyActive -> stringResource(R.string.billing_message_premium_already_active)
    BillingMessage.ConnectingToPlay -> stringResource(R.string.billing_message_connecting_to_play)
    BillingMessage.ProductNotConfigured -> stringResource(R.string.billing_message_product_not_configured)
    BillingMessage.OfferUnavailableForAccount -> stringResource(R.string.billing_message_offer_unavailable_account)
    BillingMessage.PurchaseCanceled -> stringResource(R.string.billing_message_purchase_canceled)
    BillingMessage.PurchasePending -> stringResource(R.string.billing_message_purchase_pending)
    BillingMessage.PurchaseRestored -> stringResource(R.string.billing_message_purchase_restored)
    BillingMessage.NoActivePurchase -> stringResource(R.string.billing_message_no_active_purchase)
    BillingMessage.PremiumActivated -> stringResource(R.string.billing_message_premium_activated)
    BillingMessage.AcknowledgementRetry -> stringResource(R.string.billing_message_acknowledgement_retry)
    BillingMessage.BillingUnavailable -> stringResource(R.string.billing_error_unavailable)
    BillingMessage.ItemUnavailable -> stringResource(R.string.billing_error_item_unavailable)
    BillingMessage.ItemAlreadyOwned -> stringResource(R.string.billing_error_item_already_owned)
    BillingMessage.NetworkUnavailable -> stringResource(R.string.billing_error_network)
    is BillingMessage.PlayError ->
        debugMessage?.takeIf(String::isNotBlank) ?: stringResource(R.string.billing_error_generic)
}

@StringRes
fun PremiumFeature.titleRes(): Int = when (this) {
    PremiumFeature.EDIT_HOME -> R.string.premium_feature_edit_home_title
    PremiumFeature.SUGGESTIONS -> R.string.premium_feature_suggestions_title
    PremiumFeature.PROFILES -> R.string.premium_feature_profiles_title
    PremiumFeature.MULTIVIEW -> R.string.premium_feature_multiview_title
    PremiumFeature.SUBTITLES_ONLINE -> R.string.premium_feature_online_subtitles_title
    PremiumFeature.MULTIPLE_SOURCES -> R.string.premium_feature_multiple_sources_title
    PremiumFeature.BACKUP -> R.string.premium_feature_backup_title
}

@StringRes
fun PremiumFeature.explanationRes(): Int = when (this) {
    PremiumFeature.EDIT_HOME -> R.string.premium_feature_edit_home_explanation
    PremiumFeature.SUGGESTIONS -> R.string.premium_feature_suggestions_explanation
    PremiumFeature.PROFILES -> R.string.premium_feature_profiles_explanation
    PremiumFeature.MULTIVIEW -> R.string.premium_feature_multiview_explanation
    PremiumFeature.SUBTITLES_ONLINE -> R.string.premium_feature_online_subtitles_explanation
    PremiumFeature.MULTIPLE_SOURCES -> R.string.premium_feature_multiple_sources_explanation
    PremiumFeature.BACKUP -> R.string.premium_feature_backup_explanation
}
