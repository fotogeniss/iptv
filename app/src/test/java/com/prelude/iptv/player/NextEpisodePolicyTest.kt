package com.prelude.iptv.player

import com.prelude.iptv.data.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NextEpisodePolicyTest {

    private fun ep(id: String) = Channel(
        name = id,
        url = "https://example.test/$id.mkv",
        kind = "series_ep"
    )

    private val keyOf: (Channel) -> String = { it.name }

    private val seasons = listOf(
        "Σεζόν 1" to listOf(ep("s1e1"), ep("s1e2")),
        "Σεζόν 2" to listOf(ep("s2e1"), ep("s2e2")),
    )

    /* ---------------- ποιο είναι το επόμενο ---------------- */

    @Test
    fun nextWithinTheSameSeason() {
        assertEquals("s1e2", NextEpisodePolicy.nextAfter(ep("s1e1"), seasons, keyOf)?.name)
    }

    @Test
    fun lastEpisodeOfSeasonContinuesIntoTheNextSeason() {
        // Το όριο σεζόν είναι το σημείο που σπάει σχεδόν πάντα.
        assertEquals("s2e1", NextEpisodePolicy.nextAfter(ep("s1e2"), seasons, keyOf)?.name)
    }

    @Test
    fun lastEpisodeOfAllHasNoNext() {
        assertNull(NextEpisodePolicy.nextAfter(ep("s2e2"), seasons, keyOf))
    }

    @Test
    fun unknownEpisodeDoesNotFallBackToTheFirst() {
        // Η ουρά αναπαραγωγής είχε ακριβώς αυτό το σφάλμα: όταν το αντικείμενο
        // δεν βρισκόταν, ο δείκτης έπεφτε στο 0 και εμφανιζόταν άσχετος τίτλος.
        assertNull(NextEpisodePolicy.nextAfter(ep("άσχετο"), seasons, keyOf))
    }

    @Test
    fun comparisonUsesKeyNotObjectIdentity() {
        // Ίδιο επεισόδιο, διαφορετικό instance — η λίστα ξαναχτίζεται συνεχώς.
        val sameEpisodeDifferentInstance = ep("s1e1").copy(logo = "άλλαξε")
        assertEquals(
            "s1e2",
            NextEpisodePolicy.nextAfter(sameEpisodeDifferentInstance, seasons, keyOf)?.name
        )
    }

    @Test
    fun emptySeasonsGiveNothing() {
        assertNull(NextEpisodePolicy.nextAfter(ep("s1e1"), emptyList(), keyOf))
    }

    /* ---------------- χρονισμός: κάρτα και αυτόματη έναρξη ---------------- */

    /** Επεισόδιο 45 λεπτών — τυπική περίπτωση. */
    private val duration = 45 * 60_000L
    private val creditsStart = duration - NextEpisodePolicy.CREDITS_TAIL_MS

    @Test
    fun cardAppearsExactlyThreeMinutesBeforeTheEnd() {
        assertFalse(
            NextEpisodePolicy.shouldOffer(duration - 180_001L, duration, hasNext = true)
        )
        assertTrue(
            NextEpisodePolicy.shouldOffer(duration - 180_000L, duration, hasNext = true)
        )
    }

    @Test
    fun autoPlayFiresExactlyOneMinuteBeforeTheEnd() {
        assertFalse(
            NextEpisodePolicy.shouldAutoPlay(duration - 60_001L, duration, hasNext = true)
        )
        assertTrue(
            NextEpisodePolicy.shouldAutoPlay(duration - 60_000L, duration, hasNext = true)
        )
    }

    @Test
    fun cardIsShownBeforeAutoPlayNeverAfter() {
        // Η σειρά των δύο γεγονότων είναι το νόημα ολόκληρης της λειτουργίας: αν
        // ποτέ αντιστρεφόταν, το επεισόδιο θα ξεκινούσε χωρίς προειδοποίηση.
        val cardAt = creditsStart - NextEpisodePolicy.CARD_LEAD_MS
        val autoAt = creditsStart + NextEpisodePolicy.AUTOPLAY_DELAY_MS
        assertTrue(cardAt < autoAt)
        assertTrue(NextEpisodePolicy.shouldOffer(autoAt, duration, hasNext = true))
    }

    @Test
    fun notOfferedInTheMiddle() {
        assertFalse(NextEpisodePolicy.shouldOffer(duration / 2, duration, hasNext = true))
    }

    @Test
    fun neverOfferedWithoutANextEpisode() {
        assertFalse(NextEpisodePolicy.shouldOffer(duration - 1_000L, duration, hasNext = false))
        assertFalse(NextEpisodePolicy.shouldAutoPlay(duration - 1_000L, duration, hasNext = false))
    }

    @Test
    fun neverOfferedWhenDurationIsUnknown() {
        // Ζωντανή ροή, ή ταινία που μόλις άνοιξε: διάρκεια 0. Χωρίς αυτόν τον
        // έλεγχο η πρόταση θα εμφανιζόταν αμέσως μόλις ξεκινήσει η αναπαραγωγή —
        // και η αυτόματη έναρξη αμέσως μετά.
        assertFalse(NextEpisodePolicy.shouldOffer(5_000L, 0L, hasNext = true))
        assertFalse(NextEpisodePolicy.shouldAutoPlay(5_000L, 0L, hasNext = true))
    }

    @Test
    fun shortClipsNeverTriggerAnything() {
        // Κλιπ δύο λεπτών: η «αρχή των τίτλων» θα έπεφτε στο πρώτο λεπτό και η
        // αυτόματη έναρξη πριν προλάβεις να δεις οτιδήποτε.
        val short = 2 * 60_000L
        assertFalse(NextEpisodePolicy.shouldOffer(80_000L, short, hasNext = true))
        assertFalse(NextEpisodePolicy.shouldAutoPlay(110_000L, short, hasNext = true))
    }

    @Test
    fun countdownNeverGoesNegative() {
        // Αν το τικ της θέσης καθυστερήσει, η κάρτα δεν πρέπει να δείχνει
        // αρνητικά δευτερόλεπτα.
        assertEquals(0, NextEpisodePolicy.autoPlayInSeconds(duration, duration))
        assertEquals(0, NextEpisodePolicy.autoPlayInSeconds(creditsStart, duration))
        assertEquals(120, NextEpisodePolicy.autoPlayInSeconds(duration - 180_000L, duration))
    }
}
