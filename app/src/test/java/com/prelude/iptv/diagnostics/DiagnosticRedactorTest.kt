package com.prelude.iptv.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticRedactorTest {
    @Test
    fun removesUrlsCredentialsNetworkIdsAndEmail() {
        val input = "https://tv.example:8080/player?username=bob&password=secret " +
            "token=abc MAC=00:11:22:33:44:55 192.168.1.2 owner@example.com"

        val safe = DiagnosticRedactor.redact(input)

        assertFalse(safe.contains("tv.example"))
        assertFalse(safe.contains("secret"))
        assertFalse(safe.contains("00:11:22:33:44:55"))
        assertFalse(safe.contains("192.168.1.2"))
        assertFalse(safe.contains("owner@example.com"))
        assertTrue(safe.contains("REDACTED"))
    }

    @Test
    fun limitsThrowableStackAndSanitizesMessage() {
        val failure = IllegalStateException("Failed https://private.example/live?token=secret")
        failure.stackTrace = Array(40) { index ->
            StackTraceElement("com.prelude.Test", "method$index", "Test.kt", index)
        }

        val report = DiagnosticRedactor.fromThrowable(failure, 123L)

        assertFalse(report.summary.contains("private.example"))
        assertFalse(report.summary.contains("token"))
        assertTrue(report.stackSummary.lines().size <= 24)
        assertTrue(report.exceptionType.contains("IllegalStateException"))
    }
}
