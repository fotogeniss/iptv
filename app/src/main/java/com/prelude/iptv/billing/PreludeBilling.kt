package com.prelude.iptv.billing

import android.content.Context

/** Application-scoped access point: exactly one BillingClient per process. */
object PreludeBilling {
    @Volatile
    private var instance: PlayBillingRepository? = null

    fun initialize(context: Context): PlayBillingRepository = repository(context).also { it.start() }

    fun repository(context: Context): PlayBillingRepository =
        instance ?: synchronized(this) {
            instance ?: PlayBillingRepository(context.applicationContext).also { instance = it }
        }
}
