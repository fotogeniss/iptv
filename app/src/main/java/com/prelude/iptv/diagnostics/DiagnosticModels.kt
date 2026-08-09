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
    val message: DiagnosticsMessage? = null,
) {
    val hasPendingReport: Boolean
        get() = pendingLocalReport != null || firebaseHasUnsentReport == true
}

sealed interface DiagnosticsMessage {
    data object ConsentSavedNeedsFirebase : DiagnosticsMessage
    data object ReportingEnabled : DiagnosticsMessage
    data object ReportingDisabled : DiagnosticsMessage
    data object FirebaseNotConfigured : DiagnosticsMessage
    data object ReportQueued : DiagnosticsMessage
    data object PendingReportsDeleted : DiagnosticsMessage
}
