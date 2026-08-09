package com.prelude.iptv.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.prelude.iptv.R
import com.prelude.iptv.diagnostics.DiagnosticsMessage

@Composable
internal fun DiagnosticsMessage.localizedText(): String = when (this) {
    DiagnosticsMessage.ConsentSavedNeedsFirebase ->
        stringResource(R.string.diagnostics_message_consent_saved_needs_firebase)
    DiagnosticsMessage.ReportingEnabled ->
        stringResource(R.string.diagnostics_message_reporting_enabled)
    DiagnosticsMessage.ReportingDisabled ->
        stringResource(R.string.diagnostics_message_reporting_disabled)
    DiagnosticsMessage.FirebaseNotConfigured ->
        stringResource(R.string.diagnostics_message_firebase_not_configured)
    DiagnosticsMessage.ReportQueued ->
        stringResource(R.string.diagnostics_message_report_queued)
    DiagnosticsMessage.PendingReportsDeleted ->
        stringResource(R.string.diagnostics_message_pending_reports_deleted)
}
