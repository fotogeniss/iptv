package com.prelude.iptv.player

import android.content.Context
import com.prelude.iptv.R

/** Android resource boundary for labels produced by either playback backend. */
internal fun Context.playerTrackLabel(
    language: String?,
    providerLabel: String?,
    fallbackIndex: Int,
): String = TrackLabelPolicy.trackLabel(
    language = language,
    label = providerLabel,
    fallbackLabel = getString(R.string.player_track_fallback, fallbackIndex),
    displayLocale = resources.configuration.locales[0],
)
