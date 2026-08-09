package com.prelude.iptv.ui.components.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsPlaybackPreferenceTest {
    @Test fun `player mode storage values normalize without changing persisted contract`() {
        assertEquals(PlayerModeOption.Automatic, PlayerModeOption.fromStorage(null))
        assertEquals(PlayerModeOption.Automatic, PlayerModeOption.fromStorage("unknown"))
        assertEquals(PlayerModeOption.ExoPlayer, PlayerModeOption.fromStorage(" EXO "))
        assertEquals("vlc", PlayerModeOption.Vlc.storageValue)
    }

    @Test fun `frame rate storage values normalize without changing persisted contract`() {
        assertEquals(AutoFrameRateOption.Off, AutoFrameRateOption.fromStorage(null))
        assertEquals(AutoFrameRateOption.Off, AutoFrameRateOption.fromStorage("unknown"))
        assertEquals(AutoFrameRateOption.Seamless, AutoFrameRateOption.fromStorage(" SEAMLESS "))
        assertEquals("always", AutoFrameRateOption.Always.storageValue)
    }
}
