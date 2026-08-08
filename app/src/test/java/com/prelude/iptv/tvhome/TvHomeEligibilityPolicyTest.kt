package com.prelude.iptv.tvhome

import com.prelude.iptv.data.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvHomeEligibilityPolicyTest {
    private fun candidate(
        kind: String = "vod",
        name: String = "Movie",
        group: String = "Movies",
        key: String = name,
        position: Long = 10 * 60_000L,
        duration: Long = 100 * 60_000L,
        engagement: Long = 1L,
        seriesKey: String = ""
    ) = TvHomeCandidate(
        profileId = 0,
        sourceId = "source",
        itemKey = key,
        channel = Channel(name = name, kind = kind, group = group, seriesId = seriesKey),
        positionMs = position,
        durationMs = duration,
        lastEngagementMs = engagement,
        seriesKey = seriesKey
    )

    @Test fun rejectsLiveAndSeriesContainers() {
        assertFalse(TvHomeEligibilityPolicy.isEligible(candidate(kind = "live")))
        assertFalse(TvHomeEligibilityPolicy.isEligible(candidate(kind = "series")))
    }

    @Test fun movieBecomesEligibleAtEarlierOfThreePercentOrTwoMinutes() {
        val duration = 30 * 60_000L // 3% = 54 sec
        assertFalse(TvHomeEligibilityPolicy.isEligible(candidate(position = 53_999L, duration = duration)))
        assertTrue(TvHomeEligibilityPolicy.isEligible(candidate(position = 54_000L, duration = duration)))
    }

    @Test fun episodeRequiresTwoMinutes() {
        assertFalse(TvHomeEligibilityPolicy.isEligible(candidate(kind = "series_ep", position = 119_999L)))
        assertTrue(TvHomeEligibilityPolicy.isEligible(candidate(kind = "series_ep", position = 120_000L)))
    }

    @Test fun rejectsCompletedOrNearlyCompletedItems() {
        val duration = 100 * 60_000L
        assertFalse(TvHomeEligibilityPolicy.isEligible(candidate(position = duration * 95 / 100, duration = duration)))
        assertFalse(TvHomeEligibilityPolicy.isEligible(candidate(position = duration - 3 * 60_000L, duration = duration)))
    }

    @Test fun excludesLockedGroupsCaseInsensitively() {
        val result = TvHomeEligibilityPolicy.select(
            listOf(candidate(group = "  ADULT ")),
            lockedGroups = setOf("adult")
        )
        assertTrue(result.isEmpty())
    }

    @Test fun newestItemsComeFirstAndResultIsCapped() {
        val input = (1L..8L).map { candidate(name = "M$it", key = "k$it", engagement = it) }
        val result = TvHomeEligibilityPolicy.select(input, emptySet())
        assertEquals(5, result.size)
        assertEquals(listOf("M8", "M7", "M6", "M5", "M4"), result.map { it.channel.name })
    }

    @Test fun onlyNewestEpisodePerSeriesIsPublished() {
        val input = listOf(
            candidate(kind = "series_ep", name = "Show S01E01", key = "e1", engagement = 10, seriesKey = "show"),
            candidate(kind = "series_ep", name = "Show S01E02", key = "e2", engagement = 20, seriesKey = "show")
        )
        val result = TvHomeEligibilityPolicy.select(input, emptySet())
        assertEquals(listOf("Show S01E02"), result.map { it.channel.name })
    }

    @Test fun duplicateIdentityIsRemoved() {
        val input = listOf(
            candidate(name = "Old", key = "same", engagement = 1),
            candidate(name = "New", key = "same", engagement = 2)
        )
        val result = TvHomeEligibilityPolicy.select(input, emptySet())
        assertEquals(listOf("New"), result.map { it.channel.name })
    }
    @Test fun hiddenItemRequiresNewerEngagementBeforeRepublishing() {
        assertFalse(TvHomeSuppressionPolicy.shouldPublish(100L, 100L))
        assertFalse(TvHomeSuppressionPolicy.shouldPublish(99L, 100L))
        assertTrue(TvHomeSuppressionPolicy.shouldPublish(101L, 100L))
        assertTrue(TvHomeSuppressionPolicy.shouldPublish(1L, null))
    }

}
