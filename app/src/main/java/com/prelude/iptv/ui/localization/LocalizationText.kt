package com.prelude.iptv.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.prelude.iptv.ui.greekUppercase

/** Uses the active app locale while preserving the Greek uppercase typography rule. */
@Composable
fun localizedUppercase(value: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return if (locale.language == "el") value.greekUppercase() else value.uppercase(locale)
}
