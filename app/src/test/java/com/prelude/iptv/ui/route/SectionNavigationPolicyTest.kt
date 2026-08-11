package com.prelude.iptv.ui.route

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Το συμβόλαιο είναι μία πρόταση: κάθε «πίσω» οδηγεί ΑΚΡΙΒΩΣ εκεί που ήταν ο
 * χρήστης πριν, ποτέ πιο πίσω και ποτέ σε σταθερό προορισμό.
 */
class SectionNavigationPolicyTest {

    private val root = listOf("home")

    /* ---------------- η βασική απαίτηση ---------------- */

    @Test
    fun backReturnsToWhereTheUserActuallyWas() {
        // Σειρές -> Ζωντανά -> πίσω πρέπει να δίνει Σειρές, όχι Αρχική.
        var stack = SectionNavigationPolicy.open(root, "series")
        stack = SectionNavigationPolicy.open(stack, "live")

        val back = SectionNavigationPolicy.back(stack)

        assertEquals(listOf("home", "series"), back)
        assertEquals("series", SectionNavigationPolicy.current(back.orEmpty()))
    }

    @Test
    fun everyStepBackUnwindsExactlyOneLevel() {
        var stack = root
        listOf("movies", "series", "live").forEach { stack = SectionNavigationPolicy.open(stack, it) }
        assertEquals(listOf("home", "movies", "series", "live"), stack)

        val steps = generateSequence(stack) { SectionNavigationPolicy.back(it) }
            .map(SectionNavigationPolicy::current)
            .toList()

        assertEquals(listOf("live", "series", "movies", "home"), steps)
    }

    /* ---------------- κατάρρευση διπλότυπων ---------------- */

    @Test
    fun revisitingASectionCollapsesInsteadOfRepeating() {
        // Αρχική -> Ταινίες -> Σειρές -> Ταινίες. Χωρίς κατάρρευση, το «πίσω»
        // θα ξαναπερνούσε από τις Σειρές — ο χρήστης το βιώνει ως «αλλού».
        var stack = root
        listOf("movies", "series", "movies").forEach { stack = SectionNavigationPolicy.open(stack, it) }

        assertEquals(listOf("home", "movies"), stack)
        assertEquals(listOf("home"), SectionNavigationPolicy.back(stack))
    }

    @Test
    fun returningToTheRootSectionEmptiesTheHistory() {
        var stack = root
        listOf("movies", "series").forEach { stack = SectionNavigationPolicy.open(stack, it) }

        stack = SectionNavigationPolicy.open(stack, "home")

        assertEquals(root, stack)
        assertFalse(SectionNavigationPolicy.canGoBack(stack))
    }

    @Test
    fun openingTheCurrentSectionIsNotNavigation() {
        val stack = SectionNavigationPolicy.open(root, "movies")
        assertEquals(stack, SectionNavigationPolicy.open(stack, "movies"))
    }

    /* ---------------- η ρίζα ---------------- */

    @Test
    fun theRootDelegatesInsteadOfPretendingToNavigate() {
        // null σημαίνει «δεν είναι δική μου απόφαση», ώστε ο καλών να μη
        // μπερδέψει το «γύρισα στην Αρχική» με το «δεν έχω πού να γυρίσω».
        assertNull(SectionNavigationPolicy.back(root))
        assertFalse(SectionNavigationPolicy.canGoBack(root))
    }

    @Test
    fun anEmptyStackIsHandledWithoutCrashing() {
        assertNull(SectionNavigationPolicy.back(emptyList()))
        assertEquals("", SectionNavigationPolicy.current(emptyList()))
        assertEquals(listOf("live"), SectionNavigationPolicy.open(emptyList(), "live"))
    }

    @Test
    fun blankSectionsAreIgnored() {
        assertEquals(root, SectionNavigationPolicy.open(root, ""))
        assertEquals(root, SectionNavigationPolicy.replaceTop(root, ""))
    }

    /* ---------------- συγχρονισμός χωρίς ιστορικό ---------------- */

    @Test
    fun replaceTopDoesNotWriteHistory() {
        // Ο τύπος περιεχομένου αλλάζει και από επαναφορά κατάστασης. Αν αυτό
        // έγραφε ιστορικό, το «πίσω» θα οδηγούσε σε ενότητα που ο χρήστης δεν
        // επισκέφθηκε ποτέ.
        val stack = SectionNavigationPolicy.open(root, "movies")

        val synced = SectionNavigationPolicy.replaceTop(stack, "series")

        assertEquals(listOf("home", "series"), synced)
        assertEquals(listOf("home"), SectionNavigationPolicy.back(synced))
    }

    @Test
    fun replaceTopOnTheRootKeepsASingleEntry() {
        val synced = SectionNavigationPolicy.replaceTop(root, "live")
        assertEquals(listOf("live"), synced)
        assertFalse(SectionNavigationPolicy.canGoBack(synced))
    }

    /* ---------------- φραγμός βάθους ---------------- */

    @Test
    fun theStackNeverGrowsWithoutBound() {
        var stack = root
        repeat(40) { index -> stack = SectionNavigationPolicy.open(stack, "section-$index") }

        assertTrue(stack.size <= SectionNavigationPolicy.MAX_DEPTH)
        assertEquals("section-39", SectionNavigationPolicy.current(stack))
    }

    @Test
    fun trimmingKeepsTheMostRecentHistory() {
        var stack = listOf("a")
        repeat(SectionNavigationPolicy.MAX_DEPTH + 2) { stack = SectionNavigationPolicy.open(stack, "s$it") }

        // Ό,τι κρατήθηκε είναι το πιο πρόσφατο, και το «πίσω» δουλεύει κανονικά.
        assertEquals(SectionNavigationPolicy.MAX_DEPTH, stack.size)
        assertEquals("s${SectionNavigationPolicy.MAX_DEPTH + 1}", SectionNavigationPolicy.current(stack))
        assertEquals(
            "s${SectionNavigationPolicy.MAX_DEPTH}",
            SectionNavigationPolicy.current(SectionNavigationPolicy.back(stack).orEmpty()),
        )
    }
}
