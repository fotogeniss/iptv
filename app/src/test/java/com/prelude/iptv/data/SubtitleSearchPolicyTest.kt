package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleSearchPolicyTest {
    @Test fun movieQueryRemovesProviderAndQualityNoise() {
        val request = SubtitleSearchPolicy.movie("GR: 4K Color Book (2024) [MULTI]")
        assertEquals("Color Book", request.title)
        assertEquals(2024, request.year)
        assertEquals("movie", request.type)
    }

    @Test fun episodeQueryExtractsSeriesSeasonAndEpisode() {
        val request = SubtitleSearchPolicy.fromChannel(
            Channel(name = "Chernobyl S01 E02", group = "SERIES | HBO", kind = "series_ep")
        )
        assertEquals("Chernobyl", request.title)
        assertEquals(1, request.season)
        assertEquals(2, request.episode)
        assertEquals("episode", request.type)
    }

    @Test fun explicitSeriesContextSupportsPlainEpisodeTitles() {
        val request = SubtitleSearchPolicy.episode("Medical Police", "2020", 1, 3)
        assertEquals("Medical Police", request.title)
        assertEquals(2020, request.year)
        assertEquals(1, request.season)
        assertEquals(3, request.episode)
    }
    @Test fun episodeApiParametersKeepStructuredIdentity() {
        val params = SubtitleSearchPolicy.episode("Chernobyl", "2019", 1, 2).apiParameters("el")
        assertEquals("Chernobyl", params["query"])
        assertEquals("episode", params["type"])
        assertEquals("2019", params["year"])
        assertEquals("1", params["season_number"])
        assertEquals("2", params["episode_number"])
    }

    @Test fun playbackContextExpandsEpisodeOnlyName() {
        val request = SubtitleSearchPolicy.fromPlayback(
            channel = Channel(name = "S01E03", group = "Season 1", kind = "series_ep"),
            seriesTitle = "Hometown Cha-Cha-Cha (2021) DE",
            yearHint = "2021",
        )
        assertEquals("Hometown Cha-Cha-Cha", request.title)
        assertEquals(2021, request.year)
        assertEquals(1, request.season)
        assertEquals(3, request.episode)
        assertEquals("Hometown Cha-Cha-Cha S01E03", request.displayQuery())
    }

    @Test fun manualEpisodeQueryKeepsStructuredIdentity() {
        val fallback = SubtitleSearchPolicy.episode("Hometown Cha-Cha-Cha", "2021", 1, 3)
        val request = SubtitleSearchPolicy.manual("Hometown Cha-Cha-Cha S01E03", fallback)
        assertEquals("Hometown Cha-Cha-Cha", request.title)
        assertEquals(1, request.season)
        assertEquals(3, request.episode)
        assertEquals("episode", request.type)
    }

}
