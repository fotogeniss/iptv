package com.prelude.iptv.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prelude.iptv.BuildConfig

/**
 * Το τρέχον επίπεδο συνδρομής, όπως το βλέπει η διεπαφή.
 *
 * Το Android/Play state μένει εδώ, ενώ το [PremiumPolicy] παραμένει καθαρή
 * λογική που δοκιμάζεται χωρίς συσκευή.
 */
@Composable
fun rememberPremiumTier(): PremiumTier {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { PreludeBilling.repository(context) }
    val billingState by repository.state.collectAsStateWithLifecycle()
    return effectivePremiumTier(billingState.entitlement.tier)
}

/**
 * QA/debug builds are separate installable artifacts for the app owner. The
 * release BuildConfig hard-codes this flag to false and cannot enable it at
 * runtime.
 */
fun effectivePremiumTier(playTier: PremiumTier): PremiumTier =
    if (BuildConfig.PREMIUM_QA_OVERRIDE) PremiumTier.PREMIUM else playTier

fun hasQaPremiumOverride(): Boolean = BuildConfig.PREMIUM_QA_OVERRIDE

/**
 * Είναι ξεκλείδωτη μια δυνατότητα;
 *
 * Το σημείο κλήσης γράφει `if (isUnlocked(EDIT_HOME))` και δεν χρειάζεται να ξέρει
 * τίποτα για συνδρομές, αγορές ή αποθήκευση. Όταν αλλάξουν αυτά, δεν αλλάζει.
 */
@Composable
fun isUnlocked(feature: PremiumFeature): Boolean =
    PremiumPolicy.unlocked(feature, rememberPremiumTier())
