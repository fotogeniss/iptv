package com.prelude.iptv.ui.splash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashProgressPolicyTest {

    /** Τρέχει N καρέ και επιστρέφει πού έφτασε η μπάρα. */
    private fun run(frames: Int, real: Float?, from: Float = 0f, finished: Boolean = false): Float {
        var shown = from
        repeat(frames) { shown = SplashProgressPolicy.next(shown, real, finished) }
        return shown
    }

    @Test
    fun `η ολοκλήρωση γεμίζει τη μπάρα αμέσως`() {
        assertEquals(1f, SplashProgressPolicy.next(0.1f, null, finished = true), 0f)
    }

    @Test
    fun `χωρίς γνωστό ποσοστό σέρνεται προς τα πάνω`() {
        val after = run(frames = 20, real = null)
        assertTrue("Πρέπει να κινείται: $after", after > 0.05f)
    }

    @Test
    fun `χωρίς γνωστό ποσοστό δεν φτάνει ποτέ το τέρμα`() {
        // Πολλά περισσότερα καρέ από όσα θα δει ποτέ ο χρήστης.
        val after = run(frames = 5_000, real = null)
        assertTrue("Δεν επιτρέπεται να ξεπεράσει το ταβάνι: $after",
            after <= SplashProgressPolicy.CREEP_CEILING + 1e-4f)
        assertTrue(after < 1f)
    }

    @Test
    fun `δεν γυρίζει ποτέ πίσω`() {
        // Το στάδιο άλλαξε και η πραγματική τιμή έπεσε — η μπάρα δεν ακολουθεί.
        assertEquals(0.7f, SplashProgressPolicy.next(0.7f, real = 0.2f, finished = false), 1e-6f)
    }

    @Test
    fun `ούτε όταν χαθεί εντελώς το ποσοστό`() {
        // Είμαστε ήδη πάνω από το ταβάνι του «σερνάμενου» — δεν πέφτουμε σε αυτό.
        val shown = 0.95f
        assertEquals(shown, SplashProgressPolicy.next(shown, real = null, finished = false), 1e-6f)
    }

    @Test
    fun `πλησιάζει την πραγματική τιμή χωρίς να πηδά`() {
        val oneFrame = SplashProgressPolicy.next(0.1f, real = 0.8f, finished = false)
        assertTrue("Κινήθηκε: $oneFrame", oneFrame > 0.1f)
        assertTrue("Δεν πήδηξε: $oneFrame", oneFrame < 0.3f)
    }

    @Test
    fun `τελικά φτάνει την πραγματική τιμή`() {
        val after = run(frames = 400, real = 0.6f)
        assertEquals(0.6f, after, 0.01f)
    }

    @Test
    fun `η πραγματική τιμή δεν σπρώχνει πάνω από το ταβάνι πριν τελειώσει`() {
        // Ακόμη κι αν η πηγή πει «100%», η εφαρμογή δεν είναι έτοιμη μέχρι να
        // το πει το finished — υπάρχει ακόμη parsing και ταξινόμηση.
        val after = run(frames = 2_000, real = 1f)
        assertTrue(after <= SplashProgressPolicy.CREEP_CEILING + 1e-4f)
    }

    @Test
    fun `τιμές εκτός ορίων δεν χαλούν τη μπάρα`() {
        assertTrue(SplashProgressPolicy.next(-5f, real = -2f, finished = false) >= 0f)
        assertTrue(SplashProgressPolicy.next(9f, real = 4f, finished = false) <= 1f)
    }

    @Test
    fun `δεν φεύγει πριν προλάβει να ειπωθεί`() {
        assertTrue(SplashProgressPolicy.remainingMs(finished = true, visibleMs = 200) > 0L)
    }

    @Test
    fun `όσο φορτώνει περιμένει μέχρι το πάνω όριο`() {
        val remaining = SplashProgressPolicy.remainingMs(finished = false, visibleMs = 1_000)
        assertEquals(SplashProgressPolicy.MAX_VISIBLE_MS - 1_000, remaining)
    }

    @Test
    fun `φεύγει όταν τελείωσε και έχει ειπωθεί`() {
        assertEquals(
            0L,
            SplashProgressPolicy.remainingMs(
                finished = true,
                visibleMs = SplashProgressPolicy.MIN_VISIBLE_MS + 1_000
            )
        )
    }

    @Test
    fun `το πάνω όριο διώχνει την εισαγωγή ακόμη κι αν φορτώνει`() {
        // Αργός πάροχος: η οθόνη φεύγει, η φόρτωση συνεχίζει από πίσω. Χωρίς αυτό
        // ο χρήστης κοιτά ένα λογότυπο που δεν εξηγεί τίποτα.
        assertEquals(
            0L,
            SplashProgressPolicy.remainingMs(finished = false, visibleMs = SplashProgressPolicy.MAX_VISIBLE_MS)
        )
        assertEquals(0L, SplashProgressPolicy.remainingMs(finished = false, visibleMs = 5 * 60_000))
    }

    @Test
    fun `το πάνω όριο υπερισχύει του κάτω`() {
        // Η φόρτωση κράτησε όσο και το πάνω όριο: δεν περιμένουμε ΚΑΙ τα 2,6
        // δευτερόλεπτα από πάνω.
        assertEquals(
            0L,
            SplashProgressPolicy.remainingMs(finished = true, visibleMs = SplashProgressPolicy.MAX_VISIBLE_MS)
        )
    }

    @Test
    fun `η αναμονή δεν ξεπερνά ποτέ το πάνω όριο`() {
        (0L..SplashProgressPolicy.MAX_VISIBLE_MS step 250).forEach { visible ->
            listOf(true, false).forEach { finished ->
                val remaining = SplashProgressPolicy.remainingMs(finished, visible)
                assertTrue(
                    "visible=$visible finished=$finished remaining=$remaining",
                    visible + remaining <= SplashProgressPolicy.MAX_VISIBLE_MS
                )
            }
        }
    }
}
