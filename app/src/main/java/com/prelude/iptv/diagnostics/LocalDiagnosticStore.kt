package com.prelude.iptv.diagnostics

import android.content.Context

internal class LocalDiagnosticStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    var collectionEnabled: Boolean
        get() = preferences.getBoolean(KEY_COLLECTION_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_COLLECTION_ENABLED, value).apply()

    fun pendingReport(): PendingDiagnosticReport? {
        val capturedAt = preferences.getLong(KEY_CAPTURED_AT, 0L)
        if (capturedAt <= 0L) return null
        return PendingDiagnosticReport(
            capturedAtMillis = capturedAt,
            exceptionType = preferences.getString(KEY_EXCEPTION_TYPE, null).orEmpty(),
            summary = preferences.getString(KEY_SUMMARY, null).orEmpty(),
            stackSummary = preferences.getString(KEY_STACK, null).orEmpty(),
        )
    }

    fun save(report: PendingDiagnosticReport) {
        preferences.edit()
            .putLong(KEY_CAPTURED_AT, report.capturedAtMillis)
            .putString(KEY_EXCEPTION_TYPE, report.exceptionType)
            .putString(KEY_SUMMARY, report.summary)
            .putString(KEY_STACK, report.stackSummary)
            .commit()
    }

    fun clearPendingReport() {
        preferences.edit()
            .remove(KEY_CAPTURED_AT)
            .remove(KEY_EXCEPTION_TYPE)
            .remove(KEY_SUMMARY)
            .remove(KEY_STACK)
            .apply()
    }

    private companion object {
        const val PREFERENCES = "diagnostics_privacy"
        const val KEY_COLLECTION_ENABLED = "crash_reporting_enabled"
        const val KEY_CAPTURED_AT = "pending_captured_at"
        const val KEY_EXCEPTION_TYPE = "pending_exception_type"
        const val KEY_SUMMARY = "pending_summary"
        const val KEY_STACK = "pending_stack"
    }
}
