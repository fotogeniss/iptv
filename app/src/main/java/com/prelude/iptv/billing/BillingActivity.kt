package com.prelude.iptv.billing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.billingActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
