package com.prelude.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackQualityPolicyTest {

    /* ---------------- ανάλυση ---------------- */

    @Test
    fun standardHeightsGetCommercialNames() {
        assertEquals("4K", PlaybackQualityPolicy.resolutionLabel(2160))
        assertEquals("1080p", PlaybackQualityPolicy.resolutionLabel(1080))
        assertEquals("720p", PlaybackQualityPolicy.resolutionLabel(720))
        assertEquals("576p", PlaybackQualityPolicy.resolutionLabel(576))
    }

    @Test
    fun croppedBroadcastHeightsAreNotDemoted() {
        // Πραγματικές ροές IPTV: 1072 αντί 1080, 1062 σε άλλες. Χωρίς ανοχή θα
        // εμφανιζόταν «720p» σε ροή που είναι στην πράξη Full HD.
        assertEquals("1080p", PlaybackQualityPolicy.resolutionLabel(1072))
        assertEquals("1080p", PlaybackQualityPolicy.resolutionLabel(1062))
        assertEquals("720p", PlaybackQualityPolicy.resolutionLabel(714))
    }

    @Test
    fun unknownHeightProducesNothing() {
        assertEquals("", PlaybackQualityPolicy.resolutionLabel(0))
    }

    /* ---------------- κωδικοποιητής ---------------- */

    @Test
    fun mimeTypesBecomeReadableNames() {
        assertEquals("H.264", PlaybackQualityPolicy.codecLabel("video/avc"))
        assertEquals("H.265", PlaybackQualityPolicy.codecLabel("video/hevc"))
        assertEquals("AV1", PlaybackQualityPolicy.codecLabel("video/av01"))
        assertEquals("MPEG-2", PlaybackQualityPolicy.codecLabel("video/mpeg2"))
    }

    @Test
    fun unknownCodecFallsBackToTheSubtypeInsteadOfBlank() {
        assertEquals("SOMETHINGNEW", PlaybackQualityPolicy.codecLabel("video/somethingnew"))
    }

    /* ---------------- ρυθμός καρέ ---------------- */

    @Test
    fun broadcastFrameRatesAreRoundedToWhatPeopleRecognise() {
        assertEquals("30 fps", PlaybackQualityPolicy.frameRateLabel(29.97f))
        assertEquals("24 fps", PlaybackQualityPolicy.frameRateLabel(23.976f))
        assertEquals("50 fps", PlaybackQualityPolicy.frameRateLabel(50f))
        assertEquals("", PlaybackQualityPolicy.frameRateLabel(0f))
    }

    /* ---------------- ρυθμός δεδομένων ---------------- */

    @Test
    fun bitrateUsesTheUnitThatFitsItsSize() {
        assertEquals("6.4 Mbps", PlaybackQualityPolicy.bitrateLabel(6_400_000))
        assertEquals("12 Mbps", PlaybackQualityPolicy.bitrateLabel(12_000_000))
        assertEquals("800 kbps", PlaybackQualityPolicy.bitrateLabel(800_000))
        assertEquals("", PlaybackQualityPolicy.bitrateLabel(0))
    }

    @Test
    fun wholeNumberBitratesDropTheTrailingDecimal() {
        // «8.0 Mbps» διαβάζεται σαν έξοδος μηχανήματος. Το κενό δεκαδικό δεν
        // προσθέτει καμία πληροφορία.
        assertEquals("8 Mbps", PlaybackQualityPolicy.bitrateLabel(8_000_000))
        assertEquals("3 Mbps", PlaybackQualityPolicy.bitrateLabel(3_000_000))
        assertEquals("1 Mbps", PlaybackQualityPolicy.bitrateLabel(1_000_000))
    }

    /* ---------------- πλήρης ετικέτα ---------------- */

    @Test
    fun fullLabelJoinsEverythingAvailable() {
        val quality = PlaybackEngine.VideoQuality(
            width = 1920, height = 1080, frameRate = 50f,
            codec = "video/avc", bitrateBps = 6_400_000
        )
        assertEquals("1080p · 50 fps · H.264 · 6.4 Mbps", PlaybackQualityPolicy.label(quality))
    }

    @Test
    fun missingPartsAreOmittedRatherThanShownAsZero() {
        // Οι περισσότερες ζωντανές ροές δεν δηλώνουν ρυθμό δεδομένων. Καλύτερα
        // σύντομη αληθινή ετικέτα παρά «1080p · 0 Mbps».
        val quality = PlaybackEngine.VideoQuality(
            width = 1920, height = 1080, frameRate = 25f, codec = "video/hevc"
        )
        assertEquals("1080p · 25 fps · H.265", PlaybackQualityPolicy.label(quality))
    }

    @Test
    fun completelyUnknownStreamProducesBlankSoNothingIsDrawn() {
        assertEquals("", PlaybackQualityPolicy.label(PlaybackEngine.VideoQuality()))
    }

    /* ---------------- ετικέτες στο μενού επιλογής ανάλυσης ---------------- */

    @Test
    fun trackLabelsAreDistinguishableWithinTheSameResolution() {
        // Το μενού ανάλυσης χτίζει ετικέτες από ανάλυση + ρυθμό δεδομένων. Μια
        // ροή HLS δίνει συχνά δύο εκδοχές του ΙΔΙΟΥ ύψους σε διαφορετικό ρυθμό·
        // χωρίς τον ρυθμό, ο χρήστης θα έβλεπε δύο φορές «1080p» και δεν θα
        // ήξερε τι διαλέγει.
        val high = listOf(
            PlaybackQualityPolicy.resolutionLabel(1080),
            PlaybackQualityPolicy.bitrateLabel(8_000_000)
        ).joinToString(" · ")
        val low = listOf(
            PlaybackQualityPolicy.resolutionLabel(1080),
            PlaybackQualityPolicy.bitrateLabel(3_000_000)
        ).joinToString(" · ")
        assertEquals("1080p · 8 Mbps", high)
        assertEquals("1080p · 3 Mbps", low)
        assert(high != low)
    }
}
