package com.prelude.iptv.diagnostics

data class PendingDiagnosticReport(
    val capturedAtMillis: Long,
    val exceptionType: String,
    val summary: String,
    val stackSummary: String,
)

data class DiagnosticsState(
    val collectionEnabled: Boolean = false,
    val firebaseConfigured: Boolean = false,
    val pendingLocalReport: PendingDiagnosticReport? = null,
    val firebaseHasUnsentReport: Boolean? = null,
    val busy: Boolean = false,
    val message: String? = null,
) {
    val hasPendingReport: Boolean
        get() = pendingLocalReport != null || firebaseHasUnsentReport == true
}
