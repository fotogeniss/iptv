package com.prelude.iptv.player

/**
 * Μεταφράζει το [VlcBackend.Snapshot] σε [PlaybackEngine.State].
 *
 * ΓΙΑΤΙ ΞΕΧΩΡΙΣΤΟ ΑΡΧΕΙΟ: το [PlaybackEngine] είναι ήδη μεγάλο και κρατά τη
 * δύσκολη λογική — φύλακα κολλήματος, επαναλήψεις, εξωτερικούς υπότιτλους. Η
 * μετάφραση ενός αντικειμένου σε άλλο δεν έχει καμία σχέση με αυτά, και μπαίνοντας
 * εκεί θα ήταν πενήντα γραμμές θορύβου ανάμεσα σε κώδικα που θέλει προσοχή.
 *
 * Είναι καθαρή συνάρτηση: δοκιμάζεται χωρίς LibVLC και χωρίς συσκευή.
 */
internal object VlcStateMapper {

    /**
     * @param previous η τρέχουσα κατάσταση — κρατά ό,τι το LibVLC δεν γνωρίζει
     * @param snapshot τι είπε το LibVLC
     * @param audioTracks διαβασμένα από τη μηχανή τη στιγμή της ενημέρωσης
     */
    fun merge(
        previous: PlaybackEngine.State,
        snapshot: VlcBackend.Snapshot,
        audioTracks: List<PlaybackEngine.TrackOption>,
        subtitleTracks: List<PlaybackEngine.TrackOption>,
    ): PlaybackEngine.State = previous.copy(
        playing = snapshot.playing,
        buffering = snapshot.buffering,
        positionMs = snapshot.positionMs,
        durationMs = snapshot.durationMs,
        // ΤΟ 0 ΔΕΝ ΓΡΑΦΕΤΑΙ ΠΑΝΩ ΑΠΟ ΓΝΩΣΤΗ ΤΙΜΗ.
        //
        // Οι διαστάσεις γίνονται γνωστές δευτερόλεπτα μετά την έναρξη. Μέχρι τότε
        // το snapshot έχει μηδενικά, και αντιγράφοντάς τα θα σβήναμε την αναλογία
        // που είχε ήδη υπολογιστεί — η εικόνα θα «πηδούσε» σε 16:9 και πίσω.
        videoAspect = snapshot.videoAspect.takeIf { it > 0f } ?: previous.videoAspect,
        quality = if (snapshot.width > 0) {
            PlaybackEngine.VideoQuality(
                width = snapshot.width,
                height = snapshot.height,
                frameRate = snapshot.frameRate,
            )
        } else previous.quality,
        // Μετρητής, όχι σημαία: η διεπαφή περιμένει αύξηση για να πάψει να δείχνει
        // το παγωμένο καρέ. Δες [PlaybackEngine.State.renderedFrames].
        renderedFrames = if (snapshot.renderedFrame && previous.renderedFrames == 0) {
            1
        } else previous.renderedFrames,
        audioTracks = audioTracks,
        subtitleTracks = subtitleTracks,
        error = snapshot.error,
    )
}
