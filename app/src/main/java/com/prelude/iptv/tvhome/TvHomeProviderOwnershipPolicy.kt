package com.prelude.iptv.tvhome

import java.util.UUID

/**
 * Validates the opaque identifier stored in Android TV Provider rows before
 * this app mutates or deletes a row. A broadcast containing another app's row
 * id must never be enough to authorize a delete through our WRITE_EPG_DATA
 * permission.
 */
internal object TvHomeProviderOwnershipPolicy {
    const val WATCH_NEXT_PREFIX = "upl:"
    const val MY_LIST_PREFIX = "upl:list:"

    fun ownedToken(providerId: String?, expectedPrefix: String): String? {
        val raw = providerId.orEmpty()
        if (!raw.startsWith(expectedPrefix)) return null
        val token = raw.removePrefix(expectedPrefix)
        if (token != token.trim()) return null
        val uuid = runCatching { UUID.fromString(token) }.getOrNull() ?: return null
        return uuid.toString().takeIf { it == token }
    }
}
