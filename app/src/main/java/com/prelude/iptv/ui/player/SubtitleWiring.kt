package com.prelude.iptv.ui.player

import android.content.Context
import com.prelude.iptv.R
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.SubtitleClient
import com.prelude.iptv.data.SubtitleSearchPolicy
import com.prelude.iptv.data.SubtitleResultNamePolicy
import com.prelude.iptv.data.PlaybackPreferencePolicy
import com.prelude.iptv.data.PlaylistStore
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.net.ProviderCancellation
import com.prelude.iptv.player.PlaybackEngine
import com.prelude.iptv.player.Cue
import com.prelude.iptv.player.SrtParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Η καλωδίωση των υποτίτλων από το διαδίκτυο, σε ΕΝΑ σημείο.
 *
 * ΓΙΑΤΙ ΞΕΧΩΡΙΣΤΑ: αυτές οι τρεις συναρτήσεις ήταν ενενήντα γραμμές γραμμένες
 * μέσα στο σημείο κλήσης. Με δεύτερο σημείο κλήσης (τον player που ανοίγει από
 * Intent) θα γινόταν το κλασικό λάθος αυτής της εφαρμογής: η ίδια λογική δύο
 * φορές, με μια διόρθωση να μπαίνει στο ένα αντίγραφο και το άλλο να μένει.
 *
 * Καμία δεν είναι `@Composable`: είναι δίκτυο και αρχεία. Ο καλών τις δίνει ως
 * λάμδα στο [PlayerHost] και δεν ξέρει τίποτα για OpenSubtitles.
 */
object SubtitleWiring {

    private fun readDownloadedCues(context: Context, uri: android.net.Uri): List<Cue> =
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            SrtParser.parse(reader.readText())
        }.orEmpty()

    /**
     * Αυτόματη λήψη: το πρώτο πράγμα που ταιριάζει, ελληνικά κατά προτίμηση.
     *
     * Επιστρέφει το μήνυμα που θα δει ο χρήστης.
     */
    suspend fun autoFetch(
        context: Context,
        channel: Channel,
        engine: PlaybackEngine,
    ): String = withContext(Dispatchers.IO) {
        // Χωρίς κλειδί δεν υπάρχει αναζήτηση. Το λέμε ρητά αντί για «δεν
        // βρέθηκαν»: το πρόβλημα είναι ρύθμιση, όχι έλλειψη υποτίτλων, και η
        // λύση βρίσκεται αλλού.
        if (!SubtitleClient.hasKey()) {
            return@withContext context.getString(R.string.player_opensubtitles_key_required)
        }
        val year = TmdbClient.extractYear(channel.name, channel.year)
        val preferredLanguage = PlaylistStore(context).preferredSubtitleLanguage
        val result = SubtitleClient.autoFetch(
            context,
            SubtitleSearchPolicy.fromChannel(channel, year),
            preferredLanguage
        )
            ?: return@withContext context.getString(R.string.player_subtitles_not_found)
        val (uri, language) = result
        val cues = readDownloadedCues(context, uri)
        if (cues.isEmpty()) return@withContext context.getString(R.string.player_invalid_subtitle_file)
        withContext(Dispatchers.Main) {
            engine.setExternalSubtitle(cues, "${language.uppercase()} · OpenSubtitles")
        }
        context.getString(
            if (language == "el") R.string.player_greek_subtitles_found
            else R.string.player_english_subtitles_found
        )
    }

    /**
     * Χειροκίνητη αναζήτηση: ελληνικά ΚΑΙ αγγλικά μαζί, με τη γλώσσα ορατή.
     *
     * Η αυτόματη εκδοχή σταματά στο πρώτο ελληνικό που θα βρει. Εδώ βλέπεις όλες
     * τις εκδόσεις και διαλέγεις — γιατί ο συγχρονισμός εξαρτάται από την έκδοση
     * της ταινίας, κάτι που κανένας αλγόριθμος δεν μαντεύει.
     */
    suspend fun search(
        context: Context,
        channel: Channel,
        query: String,
    ): List<ExternalSubtitle> = withContext(Dispatchers.IO) {
        if (!SubtitleClient.hasKey()) return@withContext emptyList()
        val year = TmdbClient.extractYear(channel.name, channel.year)
        // Το κείμενο του χρήστη υπερισχύει: αν το άλλαξε, ξέρει κάτι που εμείς
        // δεν ξέρουμε. Αν το άφησε ίδιο, βγαίνει το ίδιο αίτημα με την αυτόματη.
        val fallback = SubtitleSearchPolicy.fromChannel(channel, year)
        val request = if (query.isNotBlank()) SubtitleSearchPolicy.manual(query, fallback) else fallback
        val preferredLanguage = PlaylistStore(context).preferredSubtitleLanguage
        val results = ArrayList<ExternalSubtitle>()
        for (language in PlaybackPreferencePolicy.subtitleSearchLanguages(preferredLanguage)) {
            // The editable query can change while the first language is in
            // flight. Do not start the second provider request for a search
            // whose producer has already been cancelled.
            currentCoroutineContext().ensureActive()
            val candidates = try {
                SubtitleClient.search(request, language)
            } catch (error: Exception) {
                ProviderCancellation.rethrow(error, "Subtitle search cancelled")
                emptyList()
            }
            candidates.forEachIndexed { index, sub ->
                results += ExternalSubtitle(
                    id = sub.fileId,
                    // Το file_name είναι το πιο αναγνωρίσιμο όνομα έκδοσης.
                    // Αν ο πάροχος επιστρέψει κενό/τελεία/generic τιμή, η
                    // πολιτική παράγει σταθερό fallback αντί για «EL ·».
                    label = SubtitleResultNamePolicy.displayName(
                        fileName = sub.name,
                        release = sub.release,
                        featureTitle = sub.featureTitle,
                        request = request,
                        language = sub.lang.ifBlank { language },
                        ordinal = index,
                    ),
                    language = sub.lang.ifBlank { language },
                    matchPercent = sub.matchPercent,
                )
            }
        }
        results
    }

    /** Κατεβάζει και εφαρμόζει τον επιλεγμένο. Επιστρέφει μήνυμα για τον χρήστη. */
    suspend fun apply(
        context: Context,
        engine: PlaybackEngine,
        choice: ExternalSubtitle,
    ): String = withContext(Dispatchers.IO) {
        val uri = SubtitleClient.download(context, choice.id)
            ?: return@withContext context.getString(R.string.player_subtitle_download_failed)
        val cues = readDownloadedCues(context, uri)
        if (cues.isEmpty()) return@withContext context.getString(R.string.player_invalid_subtitle_file)
        withContext(Dispatchers.Main) { engine.setExternalSubtitle(cues, choice.label) }
        context.getString(R.string.player_subtitles_applied)
    }
}
