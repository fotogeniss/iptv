package com.prelude.iptv.ui.components.settings

import java.util.Locale

enum class PlayerModeOption(val storageValue: String) {
    Automatic("auto"),
    ExoPlayer("exo"),
    Vlc("vlc");

    companion object {
        fun fromStorage(value: String?): PlayerModeOption =
            entries.firstOrNull { it.storageValue == value?.trim()?.lowercase(Locale.ROOT) } ?: Automatic
    }
}

enum class AutoFrameRateOption(val storageValue: String) {
    Off("off"),
    Seamless("seamless"),
    Always("always");

    companion object {
        fun fromStorage(value: String?): AutoFrameRateOption =
            entries.firstOrNull { it.storageValue == value?.trim()?.lowercase(Locale.ROOT) } ?: Off
    }
}
