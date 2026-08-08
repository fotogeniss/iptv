package com.prelude.iptv.ui.status

enum class CatalogStatusKind { NONE, INFO, ERROR }

/**
 * Transitional boundary for the legacy catalog status transport.
 *
 * MainViewModel still publishes Greek diagnostic strings. Screens consume only
 * this typed classification; migrated surfaces never render the legacy text.
 * The classifier can be removed when the producer publishes a typed status.
 */
object CatalogStatusPolicy {
    fun kindOf(status: String): CatalogStatusKind = when {
        status.isBlank() -> CatalogStatusKind.NONE
        status.startsWith("Σφάλμα") -> CatalogStatusKind.ERROR
        else -> CatalogStatusKind.INFO
    }
}
