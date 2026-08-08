package com.prelude.iptv.tvhome

import android.content.Context
import android.content.pm.PackageManager

internal object TvHomeDevice {
    fun isTv(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}
