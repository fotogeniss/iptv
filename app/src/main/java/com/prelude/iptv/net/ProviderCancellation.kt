package com.prelude.iptv.net

import java.io.InterruptedIOException
import java.util.concurrent.CancellationException

/**
 * Preserves structured cancellation across synchronous provider helpers that
 * otherwise expose ordinary [Exception] boundaries to UI/view-model coroutines.
 */
object ProviderCancellation {
    fun rethrow(error: Exception, message: String = "Provider request cancelled") {
        if (error is CancellationException) throw error
        val detail = error.message.orEmpty().trim()
        val cancellationSignal = error is InterruptedIOException ||
            error is InterruptedException ||
            detail.equals("canceled", ignoreCase = true) ||
            detail.equals("cancelled", ignoreCase = true) ||
            detail.contains("call was canceled", ignoreCase = true) ||
            detail.contains("request canceled", ignoreCase = true) ||
            detail.contains("request cancelled", ignoreCase = true)
        if (cancellationSignal) {
            throw CancellationException(message).also { it.initCause(error) }
        }
    }
}
