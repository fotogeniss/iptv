package com.prelude.iptv.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutPolicyTest {

    private fun ids(entries: List<HomeEntry>) = entries.map { it.section.id }

    @Test
    fun `χωρίς αποθηκευμένα δίνει την προεπιλογή`() {
        assertEquals(HomeLayoutPolicy.DEFAULT.map { it.id }, ids(HomeLayoutPolicy.resolve()))
    }

    @Test
    fun `τα ζωντανα ξεκινουν κρυφα στην Αρχικη`() {
        // Η Αρχική είναι βιβλιοθήκη ταινιών και σειρών· ένα κανάλι ανάμεσα σε
        // αφίσες είναι κενό πλακίδιο. Παραμένουν στη λίστα, απλώς σβηστά.
        val out = HomeLayoutPolicy.resolve()
        assertFalse(out.first { it.section.id == HomeLayoutPolicy.NEW_LIVE }.visible)
        assertFalse(out.first { it.section.id == HomeLayoutPolicy.LIVE }.visible)
        assertTrue(out.first { it.section.id == HomeLayoutPolicy.NEW_MOVIES }.visible)
    }

    @Test
    fun `η επιλογη του χρηστη νικαει την προεπιλογη`() {
        // Μόλις υπάρχει αποθηκευμένη διάταξη, η προεπιλεγμένη απόκρυψη παύει να
        // ισχύει: μια προεπιλογή δεν ξαναγράφει απόφαση που έχει ήδη παρθεί.
        val saved = HomeLayoutPolicy.DEFAULT.map { it.id }
        val out = HomeLayoutPolicy.resolve(saved)
        assertTrue(out.first { it.section.id == HomeLayoutPolicy.LIVE }.visible)
    }

    @Test
    fun `στα Ζωντανα δεν κρυβονται τα ζωντανα`() {
        val out = HomeLayoutPolicy.resolve(destination = HomeLayoutPolicy.DEST_LIVE)
        assertTrue(out.first { it.section.id == HomeLayoutPolicy.LIVE }.visible)
    }

    @Test
    fun `κρυμμένη ενότητα μένει στη λίστα αλλά σημαδεμένη`() {
        val out = HomeLayoutPolicy.resolve(hidden = setOf(HomeLayoutPolicy.NEW_LIVE))
        val entry = out.first { it.section.id == HomeLayoutPolicy.NEW_LIVE }
        assertFalse(entry.visible)
        // Δεν φεύγει: αλλιώς δεν θα υπήρχε τρόπος να ξαναεμφανιστεί.
        assertTrue(out.size == HomeLayoutPolicy.DEFAULT.size)
    }

    @Test
    fun `σταθερή ενότητα δεν κρύβεται ποτέ`() {
        val hidden = HomeLayoutPolicy.toggle(emptySet(), HomeLayoutPolicy.HEADER)
        assertTrue(hidden.isEmpty())
        val out = HomeLayoutPolicy.resolve(hidden = setOf(HomeLayoutPolicy.HEADER))
        assertTrue(out.first { it.section.id == HomeLayoutPolicy.HEADER }.visible)
    }

    @Test
    fun `το μάτι ανάβει και σβήνει`() {
        val once = HomeLayoutPolicy.toggle(emptySet(), HomeLayoutPolicy.CONTINUE)
        assertEquals(setOf(HomeLayoutPolicy.CONTINUE), once)
        assertTrue(HomeLayoutPolicy.toggle(once, HomeLayoutPolicy.CONTINUE).isEmpty())
    }

    @Test
    fun `αποθηκευμένη σειρά τηρείται`() {
        val saved = listOf(
            HomeLayoutPolicy.HEADER,
            HomeLayoutPolicy.HEADER,
            HomeLayoutPolicy.SERIES,
            HomeLayoutPolicy.MOVIES,
        )
        val out = ids(HomeLayoutPolicy.resolve(saved))
        assertEquals(HomeLayoutPolicy.HEADER, out[0])
        assertEquals(1, out.count { it == HomeLayoutPolicy.HEADER })
        // Η ΣΧΕΤΙΚΗ σειρά του χρήστη τηρείται. Οι απόλυτες θέσεις όχι, και δεν
        // πρέπει: οι ενότητες που λείπουν από μια μερική αποθήκευση μπαίνουν
        // πλέον στη θέση τους αντί για το τέλος (δες το επόμενο τεστ).
        assertTrue(out.indexOf(HomeLayoutPolicy.SERIES) < out.indexOf(HomeLayoutPolicy.MOVIES))
    }

    @Test
    fun `ενότητα που λείπει μπαίνει στη ΘΕΣΗ της, όχι στο τέλος`() {
        // Παλιά αποθήκευση, πριν προστεθεί η «Νέα επεισόδια».
        val saved = HomeLayoutPolicy.DEFAULT.map { it.id } - HomeLayoutPolicy.NEW_EPISODES
        val out = ids(HomeLayoutPolicy.resolve(saved))

        assertEquals(HomeLayoutPolicy.DEFAULT.size, out.size)
        // ΤΟ ΠΡΟΗΓΟΥΜΕΝΟ ΣΥΜΒΟΛΑΙΟ ΗΤΑΝ «στο τέλος» ΚΑΙ ΗΤΑΝ ΛΑΘΟΣ. Με πειραγμένη
        // διάταξη, «το τέλος» είναι κάτω από δεκάδες ράγες κατηγοριών, όπου
        // κανείς δεν σκρολάρει· ο κάτοχος ανέφερε τις νέες ράγες ως ανύπαρκτες
        // ενώ απλώς ήταν αθέατες.
        assertEquals(HomeLayoutPolicy.DEFAULT.map { it.id }, out)
    }

    @Test
    fun `νέα ενότητα βρίσκει τη θέση της χωρίς να χαλάσει τη σειρά του χρήστη`() {
        // Ο χρήστης έχει ανεβάσει τις Σειρές πάνω από τις Ταινίες και δεν έχει
        // ποτέ αποθηκεύσει τις «Κορυφαίες».
        val saved = (HomeLayoutPolicy.DEFAULT.map { it.id } -
            setOf(HomeLayoutPolicy.TOP_MOVIES, HomeLayoutPolicy.TOP_SERIES))
            .toMutableList()
            .also {
                it.remove(HomeLayoutPolicy.SERIES)
                it.add(it.indexOf(HomeLayoutPolicy.MOVIES), HomeLayoutPolicy.SERIES)
            }

        val out = ids(HomeLayoutPolicy.resolve(saved))

        // Οι νέες μπαίνουν αμέσως μετά τα «Νέα επεισόδια», όπως στην προεπιλογή.
        assertEquals(
            out.indexOf(HomeLayoutPolicy.NEW_EPISODES) + 1,
            out.indexOf(HomeLayoutPolicy.TOP_MOVIES),
        )
        assertEquals(
            out.indexOf(HomeLayoutPolicy.TOP_MOVIES) + 1,
            out.indexOf(HomeLayoutPolicy.TOP_SERIES),
        )
        // Και η επιλογή του χρήστη μένει ανέπαφη.
        assertTrue(out.indexOf(HomeLayoutPolicy.SERIES) < out.indexOf(HomeLayoutPolicy.MOVIES))
    }

    @Test
    fun `καθε προορισμος δειχνει μονο οσα μπορουν να υπαρξουν εκει`() {
        // Ήταν το αρχικό παράπονο: ο επεξεργαστής απαρίθμησε δέκα ενότητες και η
        // οθόνη έδειξε τρεις. Έξι από αυτές ήταν αδύνατες εκεί που κοιτούσε ο
        // χρήστης, γιατί το περιεχόμενό τους δεν υπάρχει σε εκείνη την οθόνη.
        val live = ids(HomeLayoutPolicy.resolve(destination = HomeLayoutPolicy.DEST_LIVE))
        assertFalse(live.contains(HomeLayoutPolicy.NEW_EPISODES))
        assertFalse(live.contains(HomeLayoutPolicy.TOP_MOVIES))
        assertTrue(live.contains(HomeLayoutPolicy.NEW_LIVE))
        assertTrue(live.contains(HomeLayoutPolicy.LIVE))

        val movies = ids(HomeLayoutPolicy.resolve(destination = HomeLayoutPolicy.DEST_MOVIES))
        assertTrue(movies.contains(HomeLayoutPolicy.TOP_MOVIES))
        assertFalse(movies.contains(HomeLayoutPolicy.TOP_SERIES))
        assertFalse(movies.contains(HomeLayoutPolicy.NEW_LIVE))

        val series = ids(HomeLayoutPolicy.resolve(destination = HomeLayoutPolicy.DEST_SERIES))
        assertTrue(series.contains(HomeLayoutPolicy.TOP_SERIES))
        assertFalse(series.contains(HomeLayoutPolicy.TOP_MOVIES))

        // Η Αρχική τα έχει όλα: εκεί συνενώνονται και οι τρεις ενότητες.
        assertEquals(
            HomeLayoutPolicy.DEFAULT.size,
            ids(HomeLayoutPolicy.resolve(destination = HomeLayoutPolicy.DEST_HOME)).size,
        )
    }

    @Test
    fun `αποθηκευμενη ενοτητα ξενη προς τον προορισμο δεν διαρρεει`() {
        // Ένα παλιό αρχείο διάταξης περιέχει ΟΛΑ τα id, αφού μέχρι τώρα υπήρχε
        // μία κοινή ρύθμιση. Διαβασμένο ως «Ζωντανά» δεν επιτρέπεται να φέρει
        // μαζί του ενότητες ταινιών και σειρών.
        val legacy = HomeLayoutPolicy.DEFAULT.map { it.id }
        val live = ids(HomeLayoutPolicy.resolve(legacy, destination = HomeLayoutPolicy.DEST_LIVE))

        assertEquals(HomeLayoutPolicy.allowedIn(HomeLayoutPolicy.DEST_LIVE).map { it.id }, live)
    }

    @Test
    fun `άγνωστο id αγνοείται`() {
        val out = ids(HomeLayoutPolicy.resolve(listOf("κάτι-που-καταργήθηκε", HomeLayoutPolicy.SERIES)))
        assertFalse(out.contains("κάτι-που-καταργήθηκε"))
        assertEquals(HomeLayoutPolicy.DEFAULT.size, out.size)
    }

    @Test
    fun `διπλότυπο id δεν διπλασιάζει τη γραμμή`() {
        val saved = listOf(HomeLayoutPolicy.SERIES, HomeLayoutPolicy.SERIES)
        val out = ids(HomeLayoutPolicy.resolve(saved))
        assertEquals(HomeLayoutPolicy.DEFAULT.size, out.size)
        assertEquals(1, out.count { it == HomeLayoutPolicy.SERIES })
    }

    @Test
    fun `η σταθερή ανεβαίνει στην κορυφή ακόμη κι αν το αρχείο λέει αλλιώς`() {
        val saved = listOf(HomeLayoutPolicy.SERIES, HomeLayoutPolicy.MOVIES, HomeLayoutPolicy.HEADER)
        val out = ids(HomeLayoutPolicy.resolve(saved))
        assertEquals(HomeLayoutPolicy.HEADER, out[0])
        assertEquals(HomeLayoutPolicy.SERIES, out[1])
    }

    @Test
    fun `μετακίνηση αλλάζει θέση`() {
        val order = HomeLayoutPolicy.DEFAULT.map { it.id }
        val moved = HomeLayoutPolicy.move(order, from = order.lastIndex, to = HomeLayoutPolicy.FIXED_COUNT)
        assertEquals(HomeLayoutPolicy.SERIES, moved[HomeLayoutPolicy.FIXED_COUNT])
        assertEquals(order.size, moved.size)
    }

    @Test
    fun `μετακίνηση δεν περνά πάνω από τις σταθερές`() {
        val order = HomeLayoutPolicy.DEFAULT.map { it.id }
        val moved = HomeLayoutPolicy.move(order, from = order.lastIndex, to = 0)
        assertEquals(HomeLayoutPolicy.HEADER, moved[0])
        // Σταματά στο πρώτο επιτρεπτό σημείο αντί να αγνοηθεί.
        assertEquals(HomeLayoutPolicy.SERIES, moved[HomeLayoutPolicy.FIXED_COUNT])
        assertEquals(1, moved.count { it == HomeLayoutPolicy.HEADER })
    }

    @Test
    fun `σταθερή γραμμή δεν σύρεται`() {
        val order = HomeLayoutPolicy.DEFAULT.map { it.id }
        assertEquals(order, HomeLayoutPolicy.move(order, from = 0, to = 5))
    }

    @Test
    fun `δείκτης εκτός ορίων δεν χαλά τη λίστα`() {
        val order = HomeLayoutPolicy.DEFAULT.map { it.id }
        assertEquals(order, HomeLayoutPolicy.move(order, from = 3, to = 99))
        assertEquals(order, HomeLayoutPolicy.move(order, from = -1, to = 3))
        assertEquals(order, HomeLayoutPolicy.move(order, from = 3, to = 3))
    }

    @Test
    fun `σταθερή είναι μόνο η κεφαλίδα`() {
        assertEquals(1, HomeLayoutPolicy.FIXED_COUNT)
        assertEquals(HomeLayoutPolicy.HEADER, HomeLayoutPolicy.DEFAULT[0].id)
        assertEquals(HomeLayoutPolicy.HERO, HomeLayoutPolicy.DEFAULT[1].id)
    }

    @Test
    fun `τα καθαριζόμενα είναι μόνο τα ιστορικά`() {
        val clearable = HomeLayoutPolicy.DEFAULT.filter { it.clearable }.map { it.id }
        assertEquals(listOf(HomeLayoutPolicy.CONTINUE, HomeLayoutPolicy.RECENT_LIVE), clearable)
    }

    @Test
    fun `κατηγορία διαλέγεται μόνο στα τρία μεγάλα rails`() {
        val categorised = HomeLayoutPolicy.DEFAULT.filter { it.categorised }.map { it.id }
        assertEquals(
            listOf(HomeLayoutPolicy.LIVE, HomeLayoutPolicy.MOVIES, HomeLayoutPolicy.SERIES),
            categorised
        )
    }

    @Test
    fun `η σειρά επιβιώνει σε αποθήκευση και επαναφόρτωση`() {
        var order = HomeLayoutPolicy.DEFAULT.map { it.id }
        order = HomeLayoutPolicy.move(order, order.lastIndex, HomeLayoutPolicy.FIXED_COUNT)
        val hidden = HomeLayoutPolicy.toggle(emptySet(), HomeLayoutPolicy.NEW_LIVE)
        val reloaded = HomeLayoutPolicy.resolve(order, hidden)
        assertEquals(order, HomeLayoutPolicy.idsOf(reloaded))
        assertFalse(reloaded.first { it.section.id == HomeLayoutPolicy.NEW_LIVE }.visible)
    }
}
