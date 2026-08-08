package com.prelude.iptv.net

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Interruptible, bounded delay between synchronous provider download attempts. */
internal object ProviderRetryBackoff {
    private const val STEP_MS = 500L
    private const val MAX_DELAY_MS = 4_000L

    fun delayMs(attempt: Int): Long =
        (STEP_MS * attempt.coerceAtLeast(1).toLong()).coerceAtMost(MAX_DELAY_MS)

    /**
     * Uses an interruptible timed wait instead of sleeping blindly. Cancelling
     * the owning IO job interrupts the worker and propagates [InterruptedException].
     */
    @Throws(InterruptedException::class)
    fun await(attempt: Int, wait: (Long) -> Unit = ::waitInterruptibly) {
        wait(delayMs(attempt))
    }

    private fun waitInterruptibly(delayMs: Long) {
        CountDownLatch(1).await(delayMs, TimeUnit.MILLISECONDS)
    }
}
