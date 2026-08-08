package com.prelude.iptv.diagnostics

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.prelude.iptv.BuildConfig

/** Thin boundary around Firebase so the rest of diagnostics remains testable. */
internal class FirebaseCrashReporter(private val context: Context) {
    val configured: Boolean
        get() = BuildConfig.FIREBASE_CRASH_REPORTING_CONFIGURED

    fun initializeIfConfigured(): FirebaseCrashlytics? {
        if (!configured) return null
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context) ?: return null
        }
        return runCatching { FirebaseCrashlytics.getInstance() }.getOrNull()
    }

    fun currentInstance(): FirebaseCrashlytics? {
        if (!configured || FirebaseApp.getApps(context).isEmpty()) return null
        return runCatching { FirebaseCrashlytics.getInstance() }.getOrNull()
    }
}
