package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogNormalizerLargeDatasetTest {
    @Test
    fun fiftyThousandLiveRowsAreDeduplicatedWithoutLosingOrder() {
        val unique = 40_000
        val raw = ArrayList<Channel>(50_000)
        repeat(unique) { index ->
            raw += Channel(
                name = "Channel $index",
                group = "Group ${index % 100}",
                streamId = index.toString(),
                url = "http://example.invalid/live/$index",
            )
        }
        repeat(10_000) { index -> raw += raw[index] }

        val normalized = CatalogNormalizer.normalize("live", raw).items

        assertEquals(unique, normalized.size)
        assertEquals("Channel 0", normalized.first().name)
        assertEquals("Channel ${unique - 1}", normalized.last().name)
        assertEquals(unique, normalized.map(Channel::streamId).toSet().size)
    }

    @Test
    fun largeSeriesDatasetBuildsOneParentPerSeriesAndOrderedEpisodes() {
        val seriesCount = 1_000
        val episodesPerSeries = 20
        val raw = buildList(seriesCount * episodesPerSeries) {
            repeat(seriesCount) { series ->
                repeat(episodesPerSeries) { episode ->
                    add(
                        Channel(
                            name = "Series $series S01E${(episode + 1).toString().padStart(2, '0')}",
                            group = "Series",
                            url = "http://example.invalid/series/$series/$episode",
                            kind = "series_ep",
                        )
                    )
                }
            }
        }

        val normalized = CatalogNormalizer.normalize("series", raw)

        assertEquals(seriesCount, normalized.items.size)
        assertEquals(seriesCount, normalized.seriesEpisodes.size)
        assertTrue(normalized.seriesEpisodes.values.all { seasons ->
            seasons.size == 1 && seasons.single().second.size == episodesPerSeries
        })
    }
}
