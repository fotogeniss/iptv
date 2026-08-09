package com.prelude.iptv.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsStateTest {
    @Test
    fun `diagnostics remain opt in with no pending report by default`() {
        val state = DiagnosticsState()

        assertFalse(state.collectionEnabled)
        assertFalse(state.hasPendingReport)
    }

    @Test
    fun `either local or Firebase pending evidence exposes the pending action`() {
        val local = PendingDiagnosticReport(1L, "type", "summary", "stack")

        assertTrue(DiagnosticsState(pendingLocalReport = local).hasPendingReport)
        assertTrue(DiagnosticsState(firebaseHasUnsentReport = true).hasPendingReport)
    }

    @Test
    fun `status feedback crosses the state boundary as a typed identity`() {
        val state = DiagnosticsState(message = DiagnosticsMessage.ReportingEnabled)

        assertEquals(DiagnosticsMessage.ReportingEnabled, state.message)
    }
}
