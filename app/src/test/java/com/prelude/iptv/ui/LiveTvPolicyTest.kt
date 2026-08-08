package com.prelude.iptv.ui

import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.EpgManager
import com.prelude.iptv.ui.components.live.liveFilterOptions
import com.prelude.iptv.ui.components.live.liveRemaining
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveTvPolicyTest {
    private val channels = listOf(
        Channel(name = "One", url = "1"),
        Channel(name = "Two", url = "2"),
        Channel(name = "Three", url = "3")
    )
    private val keyOf: (Channel) -> String = { it.url }

    @Test fun favorites_keep_provider_order() {
        val result = LiveTvPolicy.filter(channels, setOf("3", "1"), emptySet(), keyOf, "favorites")
        assertEquals(listOf("One", "Three"), result.map { it.name })
    }

    @Test fun progress_is_clamped() {
        val p = EpgManager.Prog("Show", "", 1_000, 2_000)
        assertEquals(0f, LiveTvPolicy.progress(p, 500), 0.001f)
        assertEquals(0.5f, LiveTvPolicy.progress(p, 1_500), 0.001f)
        assertEquals(1f, LiveTvPolicy.progress(p, 2_500), 0.001f)
    }

    @Test fun appOwnedFilterCopyIsResolvedOutsideTheModel() {
        val options = liveFilterOptions(
            listOf(Channel(name = "News One", url = "news", group = "News"))
        )

        assertEquals(listOf(null, null, null, "News"), options.map { it.providerLabel })
    }

    @Test fun remainingTimeIsTypedInsteadOfPreformattedCopy() {
        val programme = EpgManager.Prog("Show", "", 0, 3_720_000)

        assertEquals(1, liveRemaining(programme, 0)?.hours)
        assertEquals(2, liveRemaining(programme, 0)?.minutes)
    }
}
