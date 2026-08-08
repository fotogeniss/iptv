package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class SubtitleResultPolicyTest {

    @Test
    fun blankProviderNamesGetStableMediaFallback() {
        val request = SubtitleSearchPolicy.episode("Diablero", "2018", 1, 1)

        val label = SubtitleResultNamePolicy.displayName(
            fileName = ".",
            release = " ",
            featureTitle = "subtitle",
            request = request,
            language = "el",
            ordinal = 0,
        )

        assertEquals("Diablero S01E01 · EL 1", label)
    }

    @Test
    fun realFileNameWinsOverReleaseAndFallback() {
        val label = SubtitleResultNamePolicy.displayName(
            fileName = "Diablero.S01E01.1080p.NF.WEB-DL.srt",
            release = "Diablero WEB",
            featureTitle = "Diablero",
            request = SubtitleSearchPolicy.episode("Diablero", "2018", 1, 1),
            language = "el",
            ordinal = 0,
        )

        assertEquals("Diablero.S01E01.1080p.NF.WEB-DL.srt", label)
    }

    @Test
    fun exactEpisodeScoresAboveWrongEpisode() {
        val request = SubtitleSearchPolicy.episode("Diablero", "2018", 1, 1)
        val exact = SubtitleMatchPolicy.percent(
            request = request,
            fileName = "Diablero.S01E01.1080p.WEB-DL.srt",
            release = "Diablero S01E01",
            featureTitle = "Diablero",
            year = 2018,
            season = 1,
            episode = 1,
            downloads = 2_000,
        )
        val wrong = SubtitleMatchPolicy.percent(
            request = request,
            fileName = "Diablero.S02E07.srt",
            release = "Diablero S02E07",
            featureTitle = "Diablero",
            year = 2018,
            season = 2,
            episode = 7,
            downloads = 20_000,
        )

        assertTrue(exact > wrong)
        assertTrue(exact in 1..99)
    }

    @Test
    fun unrelatedTitleSharingOneWordIsRejected() {
        val request = SubtitleSearchPolicy.movie("The Last Witness", "2024")
        assertFalse(
            SubtitleMatchPolicy.accepts(
                request,
                fileName = "The.Last.Kingdom.2024.srt",
                release = "The Last Kingdom WEB-DL",
                featureTitle = "The Last Kingdom",
                year = 2024,
                season = null,
                episode = null,
            )
        )
    }

    @Test
    fun wrongEpisodeIsRejectedEvenWithExactSeriesTitle() {
        val request = SubtitleSearchPolicy.episode("Diablero", "2018", 1, 3)
        assertFalse(
            SubtitleMatchPolicy.accepts(
                request,
                fileName = "Diablero.S01E08.srt",
                release = "Diablero S01E08",
                featureTitle = "Diablero",
                year = 2018,
                season = 1,
                episode = 8,
            )
        )
    }
}
