package com.prelude.iptv.ui.tv.browse

import com.prelude.iptv.ui.tv.browse.TvLiveBrowsePolicy.BackAction
import com.prelude.iptv.ui.tv.browse.TvLiveBrowsePolicy.ChannelAction
import com.prelude.iptv.ui.tv.browse.TvLiveBrowsePolicy.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TvLiveBrowsePolicyTest {

    private fun prog(title: String, isNow: Boolean = false, time: String = "20:00", desc: String = "") =
        LiveProgramme(time = time, title = title, description = desc, isNow = isNow)

    /* ---------------- BACK: η σειρά προτεραιότητας ---------------- */

    @Test
    fun backClosesDetailsBeforeChangingLevel() {
        assertEquals(
            BackAction.CLOSE_DETAILS,
            TvLiveBrowsePolicy.onBack(detailsOpen = true, level = Level.CHANNELS)
        )
    }

    @Test
    fun backReturnsToCategoriesFromChannels() {
        assertEquals(
            BackAction.BACK_TO_CATEGORIES,
            TvLiveBrowsePolicy.onBack(detailsOpen = false, level = Level.CHANNELS)
        )
    }

    @Test
    fun backDelegatesFromCategories() {
        assertEquals(
            BackAction.DELEGATE,
            TvLiveBrowsePolicy.onBack(detailsOpen = false, level = Level.CATEGORIES)
        )
    }

    /* ---------------- OK σε κανάλι ---------------- */

    @Test
    fun firstConfirmStartsPreview() {
        assertEquals(ChannelAction.PREVIEW, TvLiveBrowsePolicy.onChannelConfirm(previewKey = null, targetKey = "a"))
    }

    @Test
    fun secondConfirmOnSameChannelOpensFullPlayer() {
        // Ένας player στην εφαρμογή: η προεπισκόπηση δεν «μεγαλώνει», παραδίδει.
        assertEquals(ChannelAction.OPEN_PLAYER, TvLiveBrowsePolicy.onChannelConfirm(previewKey = "a", targetKey = "a"))
    }

    @Test
    fun armedMultiviewUsesSecondChannel() {
        assertEquals(
            ChannelAction.START_MULTIVIEW,
            TvLiveBrowsePolicy.onChannelConfirm(
                previewKey = "a", targetKey = "b", multiviewPrimaryKey = "a"
            )
        )
    }

    @Test
    fun multiviewIgnoresTheSameChannelTwice() {
        // Το ίδιο κανάλι δίπλα-δίπλα δεν έχει νόημα: πέφτουμε στη συνήθη ενέργεια.
        assertEquals(
            ChannelAction.OPEN_PLAYER,
            TvLiveBrowsePolicy.onChannelConfirm(
                previewKey = "a", targetKey = "a", multiviewPrimaryKey = "a"
            )
        )
    }

    @Test
    fun confirmOnDifferentChannelSwitchesPreviewInsteadOfOpening() {
        assertEquals(ChannelAction.PREVIEW, TvLiveBrowsePolicy.onChannelConfirm(previewKey = "a", targetKey = "b"))
    }

    /* ---------------- EPG: μία πηγή αλήθειας ---------------- */

    @Test
    fun currentProgrammeIsTheOneMarkedNow() {
        val list = listOf(prog("Πριν"), prog("Τώρα", isNow = true), prog("Μετά"))
        assertEquals("Τώρα", TvLiveBrowsePolicy.currentProgramme(list)?.title)
        assertEquals("Τώρα", TvLiveBrowsePolicy.nowTitle(list))
    }

    @Test
    fun noCurrentProgrammeGivesBlankTitleNotFirstEntry() {
        // Κρίσιμο: χωρίς «τώρα», ΔΕΝ επιστρέφουμε το πρώτο της λίστας — αυτό
        // ακριβώς έκανε τη λίστα και τον player να δείχνουν άλλο πρόγραμμα.
        val list = listOf(prog("Επόμενο"), prog("Μεθεπόμενο"))
        assertNull(TvLiveBrowsePolicy.currentProgramme(list))
        assertEquals("", TvLiveBrowsePolicy.nowTitle(list))
    }

    @Test
    fun nowTitleIsBlankForEmptyEpg() {
        assertEquals("", TvLiveBrowsePolicy.nowTitle(emptyList()))
    }

    /* ---------------- Αναλυτική πληροφορία ---------------- */

    @Test
    fun detailsUsesCurrentProgrammeWhenAvailable() {
        val list = listOf(prog("Τώρα", isNow = true, desc = "Περιγραφή"))
        val details = TvLiveBrowsePolicy.detailsFor(list, channelName = "FOX HD")
        assertEquals("Τώρα", details.title)
        assertEquals("Περιγραφή", details.description)
    }

    @Test
    fun detailsFallsBackToChannelNameSoDialogIsNeverEmpty() {
        val details = TvLiveBrowsePolicy.detailsFor(emptyList(), channelName = "FOX HD")
        assertEquals("FOX HD", details.title)
        assertEquals(true, details.description.isNotBlank())
    }
}
