package com.prelude.iptv.net

import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.CancellationException
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderCancellationTest {
    @Test fun `existing cancellation is rethrown unchanged`() {
        val original = CancellationException("cancel")
        val thrown = runCatching { ProviderCancellation.rethrow(original) }.exceptionOrNull()
        assertTrue(original === thrown)
    }

    @Test fun `interrupted IO becomes cancellation with original cause`() {
        val original = InterruptedIOException("socket interrupted")
        val thrown = runCatching { ProviderCancellation.rethrow(original) }.exceptionOrNull()
        assertTrue(thrown is CancellationException)
        assertTrue(original === thrown?.cause)
    }

    @Test fun `provider canceled message becomes cancellation`() {
        val original = IOException("Call was canceled")
        val thrown = runCatching { ProviderCancellation.rethrow(original) }.exceptionOrNull()
        assertTrue(thrown is CancellationException)
        assertTrue(original === thrown?.cause)
    }

    @Test fun `ordinary provider error remains available to caller fallback`() {
        ProviderCancellation.rethrow(IOException("HTTP 500"))
    }

    @Test fun `business error mentioning cancelled is not treated as coroutine cancellation`() {
        ProviderCancellation.rethrow(IOException("Subscription cancelled by provider"))
    }
}
