package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ο σκελετός είναι το συμβόλαιο: ό,τι κι αν κάνει η μεταγραφή, ΤΟ ΙΔΙΟ έργο
 * γραμμένο ελληνικά και σε οποιαδήποτε σύμβαση greeklish πρέπει να καταλήγει
 * στην ίδια συμβολοσειρά — αλλιώς το ταίριασμα με το TMDB δεν έχει νόημα.
 */
class GreeklishTitlePolicyTest {

    private fun assertSameSkeleton(greek: String, greeklish: String) {
        assertEquals(
            "«$greek» και «$greeklish» πρέπει να δίνουν ίδιο σκελετό",
            GreeklishTitlePolicy.latinSkeleton(greek),
            GreeklishTitlePolicy.latinSkeleton(greeklish),
        )
    }

    /* ---------------- ίδιος τίτλος, διαφορετικές συμβάσεις ---------------- */

    @Test
    fun theSameTitleMatchesAcrossEveryCommonGreeklishConvention() {
        // χ ως x / h / ch, και η ως i / h — τέσσερις γραφές του ίδιου τίτλου.
        assertSameSkeleton("Το Καφέ της Χαράς", "To Kafe tis Xaras")
        assertSameSkeleton("Το Καφέ της Χαράς", "To Kafe tis Haras")
        assertSameSkeleton("Το Καφέ της Χαράς", "TO KAFE THS XARAS")
        assertSameSkeleton("Το Καφέ της Χαράς", "To Kafe ths Charas")
    }

    @Test
    fun thetaMatchesWhetherWrittenThOrEight() {
        assertSameSkeleton("Ο Θανάσης", "O Thanasis")
        assertSameSkeleton("Ο Θανάσης", "O 8anasis")
    }

    @Test
    fun ambiguousVowelsCollapseToOneForm() {
        // η/ι/υ/ει/οι και ο/ω είναι αδύνατο να ξεχωριστούν από greeklish.
        assertSameSkeleton("Η Πολυκατοικία", "I Polykatoikia")
        assertSameSkeleton("Η Πολυκατοικία", "H Polykatoikia")
        assertSameSkeleton("Ψίθυροι Καρδιάς", "Psithyroi Kardias")
    }

    @Test
    fun digraphsAndDoubleLettersDoNotBreakMatching() {
        assertSameSkeleton("Σαββατογεννημένες", "Savvatogennimenes")
        assertSameSkeleton("Εγκλήματα", "Egklimata")
        assertSameSkeleton("Στο Παρά Πέντε", "Sto Para Pente")
        assertSameSkeleton("Άγριες Μέλισσες", "Agries Melisses")
    }

    @Test
    fun titlesWithoutAnyFunctionWordStillMatch() {
        assertSameSkeleton("Οι Μάγισσες της Σμύρνης", "Oi Magisses tis Smyrnis")
        assertSameSkeleton("Λατρεμένοι μου Γείτονες", "Latremenoi mou Geitones")
        assertSameSkeleton("Χαιρέτα μου τον Πλάτανο", "Xaireta mou ton Platano")
    }

    @Test
    fun thFollowedByConsonantIsTheVowelEtaNotTheta() {
        // «ths» = της. Αν το «th» καταπιεί το «η», ο σκελετός δεν ταιριάζει ποτέ
        // σε λίστες που γράφουν το «η» ως h.
        assertEquals(
            GreeklishTitlePolicy.latinSkeleton("της"),
            GreeklishTitlePolicy.latinSkeleton("ths"),
        )
        assertEquals(
            GreeklishTitlePolicy.latinSkeleton("Μαθήματα"),
            GreeklishTitlePolicy.latinSkeleton("Mathimata"),
        )
    }

    @Test
    fun differentTitlesDoNotCollide() {
        assertNotEquals(
            GreeklishTitlePolicy.latinSkeleton("Το Καφέ της Χαράς"),
            GreeklishTitlePolicy.latinSkeleton("Άγριες Μέλισσες"),
        )
        assertNotEquals(
            GreeklishTitlePolicy.latinSkeleton("Sto Para Pente"),
            GreeklishTitlePolicy.latinSkeleton("Oi Treis Xarites"),
        )
    }

    @Test
    fun skeletonIsEmptyWhenNothingComparableRemains() {
        assertEquals("", GreeklishTitlePolicy.latinSkeleton(""))
        assertEquals("", GreeklishTitlePolicy.latinSkeleton("---"))
    }

    /* ---------------- το ερώτημα προς το TMDB ---------------- */

    @Test
    fun greekQueryKeepsTheSameSkeletonAsTheRealTitle() {
        // Το ερώτημα δεν χρειάζεται να είναι ο ακριβής τίτλος, αλλά πρέπει να
        // παραμένει αναγνωρίσιμο ως το ίδιο έργο.
        listOf(
            "To Kafe tis Xaras" to "Το Καφέ της Χαράς",
            "Sto Para Pente" to "Στο Παρά Πέντε",
            "Psithyroi Kardias" to "Ψίθυροι Καρδιάς",
            "Min Arxizeis ti Mourmoura" to "Μην Αρχίζεις τη Μουρμούρα",
            "Htan oloi tous paidia mou" to "Ήταν όλοι τους παιδιά μου",
        ).forEach { (greeklish, real) ->
            assertEquals(
                "το ερώτημα για «$greeklish» πρέπει να ταυτίζεται με «$real»",
                GreeklishTitlePolicy.latinSkeleton(real),
                GreeklishTitlePolicy.latinSkeleton(GreeklishTitlePolicy.toGreek(greeklish)),
            )
        }
    }

    @Test
    fun functionWordsUseTheirRealSpelling() {
        // Γράμμα-προς-γράμμα το «tis» δίνει «τις»· σχεδόν πάντα είναι «της».
        assertTrue(GreeklishTitlePolicy.toGreek("To Kafe tis Xaras").contains("της"))
        assertTrue(GreeklishTitlePolicy.toGreek("Sto Para Pente").startsWith("στο"))
    }

    @Test
    fun queryProducesGreekLetters() {
        val query = GreeklishTitlePolicy.toGreek("Agries Melisses")
        assertTrue("περίμενα ελληνικά, βρήκα «$query»", query.any { it in 'α'..'ω' })
    }

    /* ---------------- πότε ενεργοποιείται ---------------- */

    @Test
    fun englishTitlesAreLeftAlone() {
        assertFalse(GreeklishTitlePolicy.looksGreeklish("The Office"))
        assertFalse(GreeklishTitlePolicy.looksGreeklish("Game of Thrones"))
        assertFalse(GreeklishTitlePolicy.looksGreeklish("How I Met Your Mother"))
        assertFalse(GreeklishTitlePolicy.looksGreeklish("Stranger Things Season 2"))
    }

    @Test
    fun alreadyGreekTitlesAreLeftAlone() {
        // Δουλεύουν ήδη· δεν υπάρχει λόγος για δεύτερο ερώτημα.
        assertFalse(GreeklishTitlePolicy.looksGreeklish("Το Καφέ της Χαράς"))
        assertFalse(GreeklishTitlePolicy.looksGreeklish("ΑΓΡΙΕΣ ΜΕΛΙΣΣΕΣ"))
    }

    @Test
    fun greeklishTitlesAreDetectedIncludingOnesWithoutFunctionWords() {
        assertTrue(GreeklishTitlePolicy.looksGreeklish("To Kafe tis Xaras"))
        assertTrue(GreeklishTitlePolicy.looksGreeklish("Agries Melisses"))
        assertTrue(GreeklishTitlePolicy.looksGreeklish("Savvatogennimenes"))
    }

    @Test
    fun blankInputIsNotGreeklish() {
        assertFalse(GreeklishTitlePolicy.looksGreeklish(""))
        assertFalse(GreeklishTitlePolicy.looksGreeklish("   "))
        assertFalse(GreeklishTitlePolicy.looksGreeklish("2024"))
    }

    /* ---------------- επαλήθευση αποτελέσματος ---------------- */

    private fun sameTitle(a: String, b: String) = GreeklishTitlePolicy.isSameTitle(
        GreeklishTitlePolicy.latinSkeleton(a),
        GreeklishTitlePolicy.latinSkeleton(b),
    )

    @Test
    fun aMissingArticleStillCountsAsTheSameTitle() {
        // Οι λίστες παραλείπουν συχνά το άρθρο.
        assertTrue(sameTitle("Το Καφέ της Χαράς", "Kafe tis Xaras"))
        assertTrue(sameTitle("Οι Άγριες Μέλισσες", "Agries Melisses"))
    }

    @Test
    fun anUnrelatedShowIsRejected() {
        // Αυτό είναι το σημείο που προστατεύει από λάθος περιλήψεις επεισοδίων.
        assertFalse(sameTitle("Το Καφέ της Χαράς", "Agries Melisses"))
        assertFalse(sameTitle("Sto Para Pente", "Breaking Bad"))
    }

    @Test
    fun aShortFragmentDoesNotMatchALongTitle() {
        // Χωρίς τα όρια μήκους/αναλογίας, μια σειρά με τίτλο μιας λέξης θα
        // ταίριαζε με κάθε τίτλο που την περιέχει.
        assertFalse(sameTitle("Οι Μάγισσες της Σμύρνης", "Μάγισσες"))
        assertFalse(sameTitle("Το Καφέ της Χαράς", "Χαρά"))
    }

    @Test
    fun blankSkeletonsNeverMatch() {
        assertFalse(GreeklishTitlePolicy.isSameTitle("", ""))
        assertFalse(GreeklishTitlePolicy.isSameTitle("tokafe", ""))
    }
}
