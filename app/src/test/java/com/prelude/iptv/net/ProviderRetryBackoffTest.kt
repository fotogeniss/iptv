package com.prelude.iptv.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderRetryBackoffTest {
    @Test fun `delay grows linearly and stays bounded`() {
        assertEquals(500L, ProviderRetryBackoff.delayMs(1))
        assertEquals(1_000L, ProviderRetryBackoff.delayMs(2))
        assertEquals(4_000L, ProviderRetryBackoff.delayMs(8))
        assertEquals(4_000L, ProviderRetryBackoff.delayMs(Int.MAX_VALUE))
    }

    @Test fun `invalid attempt still receives the minimum delay`() {
        assertEquals(500L, ProviderRetryBackoff.delayMs(0))
        assertEquals(500L, ProviderRetryBackoff.delayMs(-10))
    }

    @Test fun `await delegates the calculated delay without real waiting`() {
        var received = 0L
        ProviderRetryBackoff.await(3) { received = it }
        assertEquals(1_500L, received)
    }

    @Test fun `interruption is propagated to cancel the retry loop`() {
        val thrown = runCatching {
            ProviderRetryBackoff.await(1) { throw InterruptedException("cancel") }
        }.exceptionOrNull()
        assertTrue(thrown is InterruptedException)
    }
}
