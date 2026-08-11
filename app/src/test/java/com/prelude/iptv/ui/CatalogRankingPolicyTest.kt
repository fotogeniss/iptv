package com.prelude.iptv.ui

import com.prelude.iptv.data.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Οι δύο ράγες της αρχικής που υπόσχονταν σειρά και δεν την είχαν.
 *
 * Τα «Κορυφαία» τύπωναν θέσεις 1-20 πάνω σε αταξινόμητη λίστα, και τα «Νέα»
 * φιλτράριζαν με `year.length == 4` — που για πάροχο που στέλνει ολόκληρη
 * ημερομηνία (`1993-08-28`) δεν ισχύει ποτέ.
 */
class CatalogRankingPolicyTest {

    private fun item(
        name: String,
        rating: String = "",
        addedAt: String = "",
        year: String = "",
    ) = Channel(name = name, kind = "vod", url = "http://p/$name", rating = rating, addedAt = addedAt, year = year)

    /* ------------------------------------------------ βαθμολογία ---------- */

    @Test
    fun topRatedIsOrderedByRatingNotByProviderOrder() {
        val ranked = CatalogRankingPolicy.topRatedFirst(
            listOf(item("μέτρια", "5.5"), item("άριστη", "9.1"), item("καλή", "7")),
        )
        assertEquals(listOf("άριστη", "καλή", "μέτρια"), ranked.map(Channel::name))
    }

    @Test
    fun aDecimalRatingIsUnderstoodInBothWritings() {
        // Το κόμμα είναι νόμιμος δεκαδικός διαχωριστής σε ευρωπαϊκά portals.
        assertEquals(7.4, CatalogRankingPolicy.ratingOf(item("x", "7.4"))!!, 0.0001)
        assertEquals(7.4, CatalogRankingPolicy.ratingOf(item("x", "7,4"))!!, 0.0001)
    }

    @Test
    fun zeroAndUnknownAreNotRatings() {
        // Πολλά portals γράφουν "0" ή "" όταν δεν ξέρουν. Αν το δεχόμασταν, οι
        // αβαθμολόγητες ταινίες θα σχημάτιζαν ψεύτικη ουρά μέσα στην κατάταξη.
        assertNull(CatalogRankingPolicy.ratingOf(item("x", "0")))
        assertNull(CatalogRankingPolicy.ratingOf(item("x", "")))
        assertNull(CatalogRankingPolicy.ratingOf(item("x", "N/A")))
    }

    @Test
    fun itemsWithoutARatingAreLeftOutOfTheRankedRail() {
        val ranked = CatalogRankingPolicy.topRatedFirst(
            listOf(item("με", "8"), item("χωρίς"), item("μηδέν", "0")),
        )
        assertEquals(listOf("με"), ranked.map(Channel::name))
    }

    /* ------------------------------------------------ νέα ----------------- */

    @Test
    fun newestUsesWhenItWasAddedNotWhenItWasMade() {
        // Ακριβώς η περίπτωση που έκανε τη ράγα να δείχνει λάθος: η παλιότερη
        // ταινία είναι η πιο πρόσφατη προσθήκη.
        val palia = item("παλιά ταινία, νέα προσθήκη", addedAt = "2025-07-30 01:08:10", year = "1993-08-28")
        val nea = item("νέα ταινία, παλιά προσθήκη", addedAt = "2024-01-02 10:00:00", year = "2024")

        assertEquals(
            listOf("παλιά ταινία, νέα προσθήκη", "νέα ταινία, παλιά προσθήκη"),
            CatalogRankingPolicy.newestFirst(listOf(nea, palia)).map(Channel::name),
        )
    }

    @Test
    fun aFullAirDateNoLongerDisqualifiesAnItem() {
        // Το παλιό φίλτρο ήταν `year.length == 4`, οπότε αυτό δεν περνούσε ποτέ.
        val withDate = item("με ημερομηνία", year = "1993-08-28")
        assertEquals(listOf("με ημερομηνία"), CatalogRankingPolicy.newestFirst(listOf(withDate)).map(Channel::name))
    }

    @Test
    fun yearIsTheFallbackWhenTheProviderSendsNoAddedDate() {
        val older = item("παλιό", year = "2001")
        val newer = item("νεότερο", year = "2019")

        assertEquals(
            listOf("νεότερο", "παλιό"),
            CatalogRankingPolicy.newestFirst(listOf(older, newer)).map(Channel::name),
        )
    }

    @Test
    fun itemsWithNoTimeInformationAreLeftOut() {
        // Μια ράγα «Νέα» με στοιχεία αγνώστου χρόνου είναι η βιβλιοθήκη με άλλο
        // όνομα.
        val ranked = CatalogRankingPolicy.newestFirst(
            listOf(item("γνωστό", addedAt = "2025-01-01 00:00:00"), item("άγνωστο"), item("σκουπίδι", year = "N/A")),
        )
        assertEquals(listOf("γνωστό"), ranked.map(Channel::name))
    }

    @Test
    fun equalKeysPreserveProviderOrder() {
        // Σταθερή ταξινόμηση: η αρχική δεν επιτρέπεται να αναδιατάσσεται μόνη
        // της ανάμεσα σε δύο ανανεώσεις όταν τα κριτήρια είναι ίσα.
        val a = item("πρώτο", addedAt = "2025-01-01 00:00:00")
        val b = item("δεύτερο", addedAt = "2025-01-01 00:00:00")

        assertEquals(listOf("πρώτο", "δεύτερο"), CatalogRankingPolicy.newestFirst(listOf(a, b)).map(Channel::name))
        assertEquals(
            listOf("πρώτο", "δεύτερο"),
            CatalogRankingPolicy.topRatedFirst(
                listOf(a.copy(rating = "8"), b.copy(rating = "8")),
            ).map(Channel::name),
        )
    }
}
