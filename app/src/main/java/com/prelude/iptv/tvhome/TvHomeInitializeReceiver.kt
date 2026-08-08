package com.prelude.iptv.tvhome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract

/** Rebuilds launcher rows after install, launcher reset, or TV Provider reset. */
class TvHomeInitializeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TvContract.ACTION_INITIALIZE_PROGRAMS) {
            TvHomeSyncScheduler.schedule(context)
        }
    }
}
