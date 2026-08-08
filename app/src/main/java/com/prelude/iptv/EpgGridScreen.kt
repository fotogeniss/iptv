package com.prelude.iptv

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.EpgManager
import com.prelude.iptv.ui.isTvDevice
import com.prelude.iptv.ui.mobile.epg.MobileEpgScreen
import com.prelude.iptv.ui.tv.epg.TvEpgScreen

/**
 * Adaptive entry point for the Phase 11 EPG experience.
 *
 * The EPG data and callback contract remain unchanged. Only the presentation
 * switches between a touch-first mobile screen and a DPAD-first TV screen.
 */
@Composable
fun EpgGridScreen(
    channels: List<Channel>,
    onBack: () -> Unit,
    onProgramClick: (Channel, EpgManager.Prog) -> Unit,
    onChannelClick: (Channel) -> Unit,
    mobileBottomPadding: Dp = 0.dp
) {
    if (isTvDevice()) {
        TvEpgScreen(
            channels = channels,
            onBack = onBack,
            onProgramClick = onProgramClick,
            onChannelClick = onChannelClick
        )
    } else {
        MobileEpgScreen(
            channels = channels,
            onBack = onBack,
            onProgramClick = onProgramClick,
            onChannelClick = onChannelClick,
            bottomContentPadding = mobileBottomPadding
        )
    }
}
