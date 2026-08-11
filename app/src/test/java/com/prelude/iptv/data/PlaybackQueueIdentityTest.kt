package com.prelude.iptv.data

import com.prelude.iptv.player.NextEpisodePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Ταυτότητα επεισοδίου όταν ο πάροχος ΔΕΝ δίνει ξεχωριστή διεύθυνση.
 *
 * Σε Stalker/Ministra όλα τα επεισόδια μιας σεζόν μοιράζονται τον ίδιο
 * περιγραφέα `cmd` και ξεχωρίζουν μόνο από τον αριθμό που στέλνει το
 * create_link. Το [PlaybackQueue.favKey] είναι το ΜΟΝΑΔΙΚΟ κλειδί για
 * αγαπημένα, ιστορικό και αποθηκευμένη θέση, οπότε αν πέσει στο `cmd` όλη η
 * σεζόν γίνεται ένα αντικείμενο. Οι έλεγχοι εδώ κρατούν αυτή τη διάκριση
 * ζωντανή και ταυτόχρονα φρουρούν τα υπόλοιπα κλειδιά από παρενέργειες.
 */
class PlaybackQueueIdentityTest {

    /** Επεισόδιο όπως το χτίζει το `StalkerClient.buildEpisodeChannel`. */
    private fun stalkerEpisode(seasonRowId: String, episodeNum: String) = Channel(
        name = "Επεισόδιο $episodeNum",
        cmd = "/media/season_$seasonRowId.mpg",
        streamId = "$seasonRowId:$episodeNum",
        chId = episodeNum,
        kind = "series_ep",
        seriesId = "4711",
    )

    @Test
    fun episodesOfTheSameStalkerSeasonDoNotShareOneKey() {
        val first = stalkerEpisode("88", "1")
        val second = stalkerEpisode("88", "2")

        assertEquals(first.cmd, second.cmd)
        assertNotEquals(PlaybackQueue.favKey(first), PlaybackQueue.favKey(second))
    }

    @Test
    fun sameEpisodeNumberInAnotherSeasonIsStillADifferentKey() {
        // Ο αριθμός επεισοδίου επαναλαμβάνεται σε κάθε σεζόν· χωρίς το
        // seasonRowId μέσα στο streamId, S01E01 και S02E01 θα συγχωνεύονταν.
        assertNotEquals(
            PlaybackQueue.favKey(stalkerEpisode("88", "1")),
            PlaybackQueue.favKey(stalkerEpisode("89", "1")),
        )
    }

    @Test
    fun stalkerEpisodeKeyIsStableAcrossRebuiltInstances() {
        // Ο κατάλογος ξαναχτίζεται σε κάθε ανανέωση: το κλειδί πρέπει να
        // επιβιώνει, αλλιώς «χάνονται» αγαπημένα και θέσεις.
        val rebuilt = stalkerEpisode("88", "3").copy(logo = "άλλαξε", plot = "νέα περιγραφή")
        assertEquals(
            PlaybackQueue.favKey(stalkerEpisode("88", "3")),
            PlaybackQueue.favKey(rebuilt),
        )
    }

    @Test
    fun xtreamEpisodeKeepsItsUrlAsKey() {
        // Ο Xtream δίνει δική του διεύθυνση ανά επεισόδιο. Το κλειδί ΔΕΝ
        // επιτρέπεται να αλλάξει σχήμα εκεί — είναι αποθηκευμένο συμβόλαιο.
        val episode = Channel(
            name = "S01E04",
            url = "https://portal.test/series/user/pass/904.mkv",
            streamId = "904",
            kind = "series_ep",
        )
        assertEquals("https://portal.test/series/user/pass/904.mkv", PlaybackQueue.favKey(episode))
    }

    @Test
    fun liveMovieAndSeriesKeysAreUnchanged() {
        // Φρουρός συμβολαίου: μόνο τα series_ep χωρίς url άλλαξαν.
        assertEquals(
            "http://portal.test/live/9.ts",
            PlaybackQueue.favKey(Channel(name = "Κανάλι", url = "http://portal.test/live/9.ts")),
        )
        assertEquals(
            "/media/movie_5.mpg",
            PlaybackQueue.favKey(Channel(name = "Ταινία", cmd = "/media/movie_5.mpg", kind = "vod")),
        )
        assertEquals(
            "4711",
            PlaybackQueue.favKey(Channel(name = "Σειρά", seriesId = "4711", kind = "series")),
        )
    }

    @Test
    fun theProviderTmdbIdIsAnExternalReferenceAndNeverAnIdentity() {
        // Το `tmdbId` προστέθηκε ως εξωτερική αναφορά, όχι ως ταυτότητα. Ο
        // πάροχος μπορεί να το προσθέσει, να το αλλάξει ή να το αφήσει κενό
        // μεταξύ δύο ανανεώσεων καταλόγου· αν συμμετείχε σε κλειδί, αγαπημένα,
        // ιστορικό και θέσεις συνέχισης θα μετακινούνταν από μόνα τους. Αυτό
        // έχει ήδη συμβεί δύο φορές σε αυτό το repo με άλλα πεδία.
        val withoutId = stalkerEpisode("88", "5")
        val withId = withoutId.copy(tmdbId = "2328")
        val withAnotherId = withoutId.copy(tmdbId = "999999")

        assertEquals(PlaybackQueue.favKey(withoutId), PlaybackQueue.favKey(withId))
        assertEquals(PlaybackQueue.favKey(withId), PlaybackQueue.favKey(withAnotherId))
    }

    @Test
    fun aSeriesKeepsOneCatalogEntryWhicheverRowCarriedTheTmdbId() {
        // Ο normalizer δεν επιτρέπεται να δει δύο ΔΙΑΦΟΡΕΤΙΚΕΣ σειρές επειδή
        // μία γραμμή είχε tmdb_id και η άλλη όχι — θα εμφανίζονταν διπλές.
        val plain = Channel(name = "Power Rangers", seriesId = "42504", kind = "series")
        val enriched = plain.copy(tmdbId = "2328")

        val normalized = CatalogNormalizer.normalize("series", listOf(plain, enriched))

        assertEquals(1, normalized.items.size)
        // Και η μη κενή τιμή επιβιώνει της συγχώνευσης.
        assertEquals("2328", normalized.items.first().tmdbId)
    }

    @Test
    fun nextEpisodeAdvancesInsteadOfRepeatingTheSecondOfTheSeason() {
        // Η ορατή συνέπεια του παλιού κοινού κλειδιού: το nextAfter έβρισκε
        // πάντα τη θέση 0, οπότε το «επόμενο» ήταν πάντα το δεύτερο επεισόδιο
        // — και πατώντας το πάνω στο δεύτερο δεν συνέβαινε απολύτως τίποτα.
        val seasonOne = (1..4).map { stalkerEpisode("88", it.toString()) }
        val seasonTwo = (1..2).map { stalkerEpisode("89", it.toString()) }
        val seasons = listOf("Σεζόν 1" to seasonOne, "Σεζόν 2" to seasonTwo)

        assertEquals(
            "Επεισόδιο 4",
            NextEpisodePolicy.nextAfter(seasonOne[2], seasons, PlaybackQueue::favKey)?.name,
        )
    }

    @Test
    fun lastEpisodeOfASeasonCrossesIntoTheNextSeason() {
        val seasonOne = (1..3).map { stalkerEpisode("88", it.toString()) }
        val seasonTwo = (1..2).map { stalkerEpisode("89", it.toString()) }
        val seasons = listOf("Σεζόν 1" to seasonOne, "Σεζόν 2" to seasonTwo)

        val next = NextEpisodePolicy.nextAfter(seasonOne.last(), seasons, PlaybackQueue::favKey)

        assertEquals("Επεισόδιο 1", next?.name)
        assertEquals("89:1", next?.streamId)
    }
}
