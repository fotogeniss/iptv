package com.prelude.iptv.diagnostics

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide privacy boundary for stability diagnostics.
 *
 * Firebase is not initialized unless the user enables reporting or explicitly
 * sends one pending report. No media title, playlist URL, account id or custom
 * navigation breadcrumb is attached to a report.
 */
object DiagnosticsManager {
    private val mutableState = MutableStateFlow(DiagnosticsState())
    val state: StateFlow<DiagnosticsState> = mutableState.asStateFlow()

    private var initialized = false
    private lateinit var localStore: LocalDiagnosticStore
    private lateinit var firebaseReporter: FirebaseCrashReporter

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        val applicationContext = context.applicationContext
        localStore = LocalDiagnosticStore(applicationContext)
        firebaseReporter = FirebaseCrashReporter(applicationContext)
        LocalCrashCapture.install(localStore)
        initialized = true

        val enabled = localStore.collectionEnabled
        mutableState.value = DiagnosticsState(
            collectionEnabled = enabled,
            firebaseConfigured = firebaseReporter.configured,
            pendingLocalReport = localStore.pendingReport(),
        )
        if (enabled) {
            firebaseReporter.initializeIfConfigured()?.let { firebase ->
                // Keep Firebase's own automatic uploader off. The app sends
                // queued reports itself only while the persisted opt-in is true.
                firebase.setCrashlyticsCollectionEnabled(false)
                sendQueuedReportsAfterConsent(firebase, showMessage = false)
            }
        } else {
            refreshPendingState()
        }
    }

    fun setCollectionEnabled(enabled: Boolean) {
        if (!initialized) return
        localStore.collectionEnabled = enabled
        val firebase = if (enabled) {
            firebaseReporter.initializeIfConfigured()
        } else {
            firebaseReporter.currentInstance()
        }
        firebase?.setCrashlyticsCollectionEnabled(false)
        if (!enabled) firebase?.deleteUnsentReports()
        mutableState.value = mutableState.value.copy(
            collectionEnabled = enabled,
            firebaseHasUnsentReport = if (enabled) mutableState.value.firebaseHasUnsentReport else false,
            message = if (enabled && !firebaseReporter.configured) {
                "Η άδεια αποθηκεύτηκε. Χρειάζεται σύνδεση Firebase για αποστολή."
            } else if (enabled) {
                "Η αποστολή crash και ANR reports ενεργοποιήθηκε."
            } else {
                "Η αποστολή απενεργοποιήθηκε και τα reports του Firebase διαγράφηκαν."
            },
        )
        if (enabled && firebase != null) {
            sendQueuedReportsAfterConsent(firebase, showMessage = false)
        } else {
            refreshPendingState()
        }
    }

    fun refreshPendingState() {
        if (!initialized) return
        val local = localStore.pendingReport()
        val firebase = firebaseReporter.currentInstance()
        mutableState.value = mutableState.value.copy(
            pendingLocalReport = local,
            firebaseConfigured = firebaseReporter.configured,
        )
        if (firebase == null) return

        mutableState.value = mutableState.value.copy(busy = true)
        firebase.checkForUnsentReports().addOnCompleteListener { task ->
            mutableState.value = mutableState.value.copy(
                firebaseHasUnsentReport = if (task.isSuccessful) task.result == true else false,
                busy = false,
            )
        }
    }

    fun sendPendingOnce() {
        if (!initialized) return
        val firebase = firebaseReporter.initializeIfConfigured()
        if (firebase == null) {
            mutableState.value = mutableState.value.copy(
                message = "Δεν έχει συνδεθεί ακόμη Firebase project. Το report παραμένει μόνο στη συσκευή.",
            )
            return
        }

        mutableState.value = mutableState.value.copy(busy = true, message = null)
        sendQueuedReportsAfterConsent(firebase, showMessage = true)
    }

    private fun sendQueuedReportsAfterConsent(
        firebase: FirebaseCrashlytics,
        showMessage: Boolean,
    ) {
        mutableState.value = mutableState.value.copy(busy = true)
        firebase.checkForUnsentReports().addOnCompleteListener { task ->
            val alreadyCapturedByFirebase = task.isSuccessful && task.result == true
            val local = localStore.pendingReport()
            if (!alreadyCapturedByFirebase && local == null) {
                mutableState.value = mutableState.value.copy(busy = false)
                return@addOnCompleteListener
            }
            if (!alreadyCapturedByFirebase && local != null) {
                recordSanitizedLocalReport(firebase, local)
            }
            firebase.sendUnsentReports()
            localStore.clearPendingReport()
            mutableState.value = mutableState.value.copy(
                pendingLocalReport = null,
                firebaseHasUnsentReport = false,
                busy = false,
                message = if (showMessage) "Το διαγνωστικό report μπήκε στην ουρά αποστολής." else mutableState.value.message,
            )
        }
    }

    fun deletePendingReports() {
        if (!initialized) return
        localStore.clearPendingReport()
        firebaseReporter.initializeIfConfigured()?.apply {
            setCrashlyticsCollectionEnabled(false)
            deleteUnsentReports()
        }
        mutableState.value = mutableState.value.copy(
            pendingLocalReport = null,
            firebaseHasUnsentReport = false,
            busy = false,
            message = "Τα εκκρεμή reports διαγράφηκαν από τη συσκευή.",
        )
    }

    fun clearMessage() {
        mutableState.value = mutableState.value.copy(message = null)
    }

    private fun recordSanitizedLocalReport(
        firebase: FirebaseCrashlytics,
        report: PendingDiagnosticReport,
    ) {
        firebase.log("Sanitized local stack:\n${report.stackSummary}")
        firebase.recordException(
            IllegalStateException("${report.exceptionType}: ${report.summary}")
        )
    }
}
