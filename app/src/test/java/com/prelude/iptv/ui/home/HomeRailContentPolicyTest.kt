package com.prelude.iptv.ui.home

import com.prelude.iptv.data.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRailContentPolicyTest {

    private fun ch(name: String, kind: String = "vod", group: String = "") =
        Channel(name = name, kind = kind, group = group)

    @Test
    fun `τα επεισόδια μετριούνται μαζί με τις σειρές`() {
        val all = listOf(
            ch("A", "series"), ch("B", "series_ep"), ch("C", "vod"), ch("D", "live")
        )
        assertEquals(2, HomeRailContentPolicy.seriesOf(all).size)
        assertEquals(1, HomeRailContentPolicy.moviesOf(all).size)
        assertEquals(1, HomeRailContentPolicy.liveOf(all).size)
    }

    @Test
    fun `οι κατηγορίες βγαίνουν με τις μεγαλύτερες πρώτα`() {
        val all = listOf(
            ch("1", group = "Μικρή"),
            ch("2", group = "Μεγάλη"), ch("3", group = "Μεγάλη"), ch("4", group = "Μεγάλη"),
            ch("5", group = "Μεσαία"), ch("6", group = "Μεσαία"),
        )
        assertEquals(listOf("Μεγάλη", "Μεσαία", "Μικρή"), HomeRailContentPolicy.categoriesOf(all))
    }

    @Test
    fun `ισοπαλία λύνεται αλφαβητικά ώστε η σειρά να μην τρεμοπαίζει`() {
        val all = listOf(ch("1", group = "Βήτα"), ch("2", group = "Άλφα"))
        assertEquals(listOf("Άλφα", "Βήτα"), HomeRailContentPolicy.categoriesOf(all))
    }

    @Test
    fun `κενές κατηγορίες αγνοούνται`() {
        val all = listOf(ch("1", group = ""), ch("2", group = "   "), ch("3", group = "Καλή"))
        assertEquals(listOf("Καλή"), HomeRailContentPolicy.categoriesOf(all))
    }

    @Test
    fun `η αποθηκευμένη κατηγορία τηρείται όσο υπάρχει`() {
        assertEquals("Β", HomeRailContentPolicy.resolveCategory("Β", listOf("Α", "Β")))
    }

    @Test
    fun `κατηγορία που έπαψε να υπάρχει πέφτει στη μεγαλύτερη`() {
        assertEquals("Α", HomeRailContentPolicy.resolveCategory("Χάθηκε", listOf("Α", "Β")))
    }

    @Test
    fun `χωρίς επιλογή παίρνει τη μεγαλύτερη`() {
        assertEquals("Α", HomeRailContentPolicy.resolveCategory("", listOf("Α", "Β")))
    }

    @Test
    fun `χωρίς καθόλου κατηγορίες δεν σκάει`() {
        assertEquals("", HomeRailContentPolicy.resolveCategory("Κάτι", emptyList()))
    }

    @Test
    fun `τα νέα είναι τα τελευταία του καταλόγου με το νεότερο πρώτο`() {
        val all = (1..30).map { ch("Ταινία $it") }
        val newest = HomeRailContentPolicy.newest(all, limit = 3)
        assertEquals(listOf("Ταινία 30", "Ταινία 29", "Ταινία 28"), newest.map { it.name })
    }

    @Test
    fun `λίγα στοιχεία δίνουν όσα υπάρχουν χωρίς σφάλμα`() {
        val all = listOf(ch("Μία"))
        assertEquals(1, HomeRailContentPolicy.newest(all, limit = 20).size)
    }

    @Test
    fun `άδειο rail δεν φτιάχνεται καθόλου`() {
        assertNull(HomeRailContentPolicy.rail("x", "Τίτλος", emptyList()))
    }

    @Test
    fun `το rail κόβεται αλλά κρατά το σύνολο για το Όλα`() {
        val all = (1..50).map { ch("Ν$it") }
        val rail = HomeRailContentPolicy.rail("x", "Τίτλος", all)!!
        assertEquals(HomeRailContentPolicy.RAIL_LIMIT, rail.items.size)
        assertEquals(50, rail.allItems.size)
    }

    @Test
    fun `οι σημαίες περνούν στο rail`() {
        val rail = HomeRailContentPolicy.rail(
            "recent-live", "Κανάλια που είδες", listOf(ch("K", "live")),
            live = true, removable = true
        )!!
        assertTrue(rail.live)
        assertTrue(rail.removable)
    }

    @Test
    fun `οι προτάσεις αναμειγνύουν ομάδες πριν επαναλάβουν ομάδα`() {
        val all = listOf(
            ch("A1", group = "A"), ch("A2", group = "A"),
            ch("B1", group = "B"), ch("B2", group = "B"),
            ch("C1", group = "C"), ch("C2", group = "C"),
        )
        val suggestions = HomeRailContentPolicy.suggestions(all, limit = 6, seed = 42)
        assertEquals(3, suggestions.take(3).map { it.group }.toSet().size)
        assertEquals(6, suggestions.map { it.name }.toSet().size)
    }

    @Test
    fun `ίδιο seed δίνει σταθερές προτάσεις`() {
        val all = (1..12).map { ch("Ταινία $it", group = "Ομάδα ${it % 4}") }
        val first = HomeRailContentPolicy.suggestions(all, limit = 8, seed = 912)
        val second = HomeRailContentPolicy.suggestions(all, limit = 8, seed = 912)
        assertEquals(first, second)
    }

    @Test
    fun `οι προτάσεις τηρούν το όριο και αφαιρούν διπλότυπα`() {
        val duplicate = ch("Ίδιο", group = "A")
        val suggestions = HomeRailContentPolicy.suggestions(
            listOf(duplicate, duplicate, ch("Άλλο", group = "B")),
            limit = 20,
            seed = 7,
        )
        assertEquals(listOf("Ίδιο", "Άλλο").toSet(), suggestions.map { it.name }.toSet())
        assertEquals(2, suggestions.size)
    }
}
