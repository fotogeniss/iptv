package com.prelude.iptv.ui.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import com.prelude.iptv.data.Channel
import com.prelude.iptv.R
import com.prelude.iptv.data.PlaylistStore
import com.prelude.iptv.data.SubtitleSearchPolicy
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.player.NextEpisodePolicy
import com.prelude.iptv.ui.MainViewModel
import com.prelude.iptv.ui.components.rememberEpisodeMeta
import com.prelude.iptv.ui.player.MobilePlaybackOverlay
import com.prelude.iptv.ui.player.PlayerEpgDialog
import com.prelude.iptv.ui.player.PlayerExtraAction
import com.prelude.iptv.ui.player.SubtitleWiring
import com.prelude.iptv.ui.player.TvPlaybackOverlay
import com.prelude.iptv.ui.player.upcomingProgrammes

/**
 * Το επίπεδο αναπαραγωγής πάνω από την περιήγηση — ταινία, επεισόδιο ή κανάλι.
 *
 * ΓΙΑΤΙ ΞΕΧΩΡΙΣΤΟ: μέσα στο [BrowseRoute] ήταν 130 γραμμές που δεν είχαν καμία
 * σχέση με την περιήγηση. Η οθόνη είχε φτάσει τις 1.600 γραμμές επειδή κάθε νέα
 * δυνατότητα του player —υπότιτλοι, επόμενο επεισόδιο, πρόγραμμα, αγαπημένα—
 * προσγειωνόταν εκεί.
 *
 * Εδώ ζει η ΣΥΝΔΕΣΗ του player με τα δεδομένα της εφαρμογής: πού βρίσκεται το
 * URL, πού αποθηκεύεται η θέση, ποιο είναι το επόμενο επεισόδιο, πώς βρίσκονται
 * υπότιτλοι. Ο ίδιος ο player δεν ξέρει τίποτα από αυτά — και σωστά.
 *
 * @param onPlayOther άλλαξε σε άλλο περιεχόμενο χωρίς έξοδο (CH+/CH− σε ζωντανά,
 *   επόμενο επεισόδιο σε σειρές). Ο καλών κρατά το «τι παίζει».
 */
@Composable
internal fun BrowsePlaybackLayer(
    target: Channel,
    vm: MainViewModel,
    isTv: Boolean,
    favoriteKeys: Set<String>,
    seasons: List<Pair<String, List<Channel>>>,
    catalogChannels: List<Channel>,
    parentContent: Channel? = null,
    onClose: () -> Unit,
    onPlayOther: (Channel) -> Unit,
    onOpenDetails: (Channel) -> Unit,
) {
    val ctx = LocalContext.current
    val store = remember { PlaylistStore(ctx) }

    /**
     * ΜΕΣΩ ΤΟΥ ViewModel, ΟΧΙ ΑΠΕΥΘΕΙΑΣ ΣΤΟ Repository.
     *
     * Το `resolvePlayableUrl` υπάρχει ακριβώς γι' αυτή τη δουλειά: για πηγές
     * Stalker (MAC portal) η εντολή του καναλιού χρειάζεται ΖΩΝΤΑΝΗ συνεδρία με
     * τον πάροχο. Συνδέεται αν δεν υπάρχει, και ξανασυνδέεται αν το token έληξε
     * ενώ ο κατάλογος στη μνήμη είναι ακόμη έγκυρος.
     *
     * Εγώ καλούσα το `Repository.playableUrl(ch, currentStalker())` κατευθείαν —
     * που επιστρέφει null client όταν ο κατάλογος ήρθε από cache χωρίς σύνδεση.
     * Τότε πέφτει στο `ch.url`, που σε Stalker είναι κενό: οι τίτλοι και οι
     * εικόνες φαίνονταν κανονικά (έρχονται από τον κατάλογο) αλλά τίποτα δεν
     * έπαιζε. Στην τηλεόραση δούλευε μόνο όταν είχε τύχει να γίνει φρέσκια
     * φόρτωση και να υπάρχει ήδη συνεδρία.
     */
    val resolve: suspend (Channel) -> String = { channel -> vm.resolvePlayableUrl(channel) }
    val loadResume: (Channel) -> Long = { channel ->
        store.loadPosition(vm.currentSourceId(), vm.favKey(channel))?.first ?: 0L
    }
    val saveResume: (Channel, Long, Long) -> Unit = { channel, position, duration ->
        store.savePosition(vm.currentSourceId(), vm.favKey(channel), position, duration)
    }
    val infoChannel = if (target.kind == "series_ep") parentContent ?: target else target
    val playerMetadata by produceState<TmdbClient.Meta?>(null, infoChannel) {
        value = if (target.kind == "live") null else vm.tmdb(infoChannel)
    }
    val contextItems = remember(target, infoChannel, catalogChannels) {
        when (target.kind) {
            "live" -> catalogChannels
                .asSequence()
                .filter { it.kind == "live" && it.group == target.group }
                .distinctBy(vm::favKey)
                .take(24)
                .toList()
            "series_ep", "series" -> catalogChannels
                .asSequence()
                .filter { it.kind == "series" && vm.favKey(it) != vm.favKey(infoChannel) }
                .sortedByDescending { it.group.isNotBlank() && it.group == infoChannel.group }
                .distinctBy(vm::favKey)
                .take(20)
                .toList()
            else -> catalogChannels
                .asSequence()
                .filter { it.kind == "vod" && vm.favKey(it) != vm.favKey(target) }
                .sortedByDescending { it.group.isNotBlank() && it.group == target.group }
                .distinctBy(vm::favKey)
                .take(20)
                .toList()
        }
    }

    // CH+/CH− μέσα στον player: επόμενο/προηγούμενο κανάλι της ΟΡΑΤΗΣ λίστας,
    // δηλαδή αυτής που έβλεπε ο χρήστης όταν πάτησε. Μόνο για ζωντανά — σε ταινία
    // δεν έχει νόημα «επόμενο».
    val channelStep: ((Int) -> Unit)? = if (target.kind == "live") {
        { delta ->
            val list = vm.visibleChannels().filter { it.kind == "live" }
            val index = list.indexOfFirst { vm.favKey(it) == vm.favKey(target) }
            if (index >= 0) list.getOrNull(index + delta)?.let(onPlayOther)
        }
    } else null

    // Επόμενο επεισόδιο: μόνο για σειρές, και μόνο όσο η οθόνη λεπτομερειών από
    // κάτω κρατά φορτωμένες τις σεζόν. Αν δεν τις έχει (π.χ. άνοιγμα απευθείας
    // από «Συνέχισε να βλέπεις»), δεν προτείνουμε τίποτα αντί να μαντέψουμε από
    // τα ονόματα των αρχείων.
    val nextEpisode: Channel? = if (target.kind == "series_ep") {
        NextEpisodePolicy.nextAfter(current = target, seasons = seasons, keyOf = vm::favKey)
    } else null

    // ---- ΕΙΚΟΝΑ ΤΟΥ ΕΠΟΜΕΝΟΥ ΕΠΕΙΣΟΔΙΟΥ ----
    //
    // Πρώτα ό,τι δίνει ο πάροχος (άμεσο, χωρίς δίκτυο). Αν δεν δίνει τίποτα —
    // που είναι ο κανόνας στα IPTV playlists — ζητάμε από το TMDB το στιγμιότυπο
    // του συγκεκριμένου επεισοδίου.
    //
    // Η σεζόν και ο αριθμός βγαίνουν από τη ΘΕΣΗ του επεισοδίου στη λίστα και όχι
    // από το όνομα του αρχείου: τα ονόματα των παρόχων είναι ασυνεπή, ενώ η σειρά
    // της λίστας είναι αυτή που βλέπει ο χρήστης.
    val nextSeasonIndex = remember(nextEpisode, seasons) {
        seasons.indexOfFirst { (_, episodes) ->
            episodes.any { nextEpisode != null && vm.favKey(it) == vm.favKey(nextEpisode) }
        }
    }
    val nextEpisodeNumber = remember(nextEpisode, seasons, nextSeasonIndex) {
        if (nextSeasonIndex < 0 || nextEpisode == null) -1
        else seasons[nextSeasonIndex].second
            .indexOfFirst { vm.favKey(it) == vm.favKey(nextEpisode) } + 1
    }
    val nextMeta = if (nextEpisode != null && nextSeasonIndex >= 0 && nextEpisodeNumber > 0) {
        rememberEpisodeMeta(
            seriesTitle = TmdbClient.cleanTitle(infoChannel.name),
            seriesYear = playerMetadata?.year?.takeIf(String::isNotBlank) ?: infoChannel.year,
            season = SubtitleSearchPolicy.seasonNumber(
                seasons[nextSeasonIndex].first,
                nextSeasonIndex + 1,
            ) ?: nextSeasonIndex + 1,
            episodeNumber = SubtitleSearchPolicy.episodeNumber(nextEpisode.name, nextEpisodeNumber)
                ?: nextEpisodeNumber,
        )
    } else null
    val nextImageUrl = nextMeta?.still?.takeIf { it.isNotBlank() }
        ?: nextEpisode?.logo?.takeIf { it.isNotBlank() }

    // Πρόγραμμα μέσα στον player, μόνο για ζωντανά. Ο διάλογος φορτώνει όταν
    // ανοίξει — το EPG αργού portal δεν πρέπει να καθυστερεί κάθε κανάλι.
    var epgOpen by remember(target) { mutableStateOf(false) }

    // Πάνω από ΟΛΑ (και από την οθόνη λεπτομερειών, που παραμένει από κάτω ώστε
    // το BACK να επιστρέφει εκεί). Χωρίς zIndex, οι λεπτομέρειες ζωγραφίζονται
    // μετά και σκεπάζουν την εικόνα.
    val playerModifier = Modifier.zIndex(10f)

    // Ίδια μηχανή και στις δύο συσκευές — αλλάζουν μόνο τα χειριστήρια: D-pad
    // στην τηλεόραση, χειρονομίες αφής στο κινητό.
    if (isTv) {
        TvPlaybackOverlay(
            channel = target,
            title = target.name,
            subtitle = listOf(target.year, target.genre)
                .filter(String::isNotBlank).joinToString(" · "),
            resolveUrl = resolve,
            loadResumeMs = loadResume,
            saveResumeMs = saveResume,
            onClose = onClose,
            onChannelStep = channelStep,
            isFavorite = vm.favKey(target) in favoriteKeys,
            onToggleFavorite = { vm.toggleFavorite(target) },
            nextTitle = nextEpisode?.name,
            nextImageUrl = nextImageUrl,
            // Η θέση του τρέχοντος αποθηκεύεται από το overlay στο dispose· εδώ
            // αρκεί να αλλάξει ο στόχος.
            onPlayNext = nextEpisode?.let { next -> { onPlayOther(next) } },
            // Η καλωδίωση ζει στο SubtitleWiring: την μοιράζεται με τον player που
            // ανοίγει από Intent, και δεν αντέχει δύο αντίγραφα.
            fetchSubtitles = { engine -> SubtitleWiring.autoFetch(ctx, target, engine) },
            searchSubtitles = { query -> SubtitleWiring.search(ctx, target, query) },
            applySubtitle = { engine, choice -> SubtitleWiring.apply(ctx, engine, choice) },
            extraActions = if (target.kind == "live") {
                { PlayerExtraAction(stringResource(R.string.player_programme)) { epgOpen = true } }
            } else null,
            overlayOpen = epgOpen,
            modifier = playerModifier
        )
        if (epgOpen) {
            PlayerEpgDialog(
                channelName = target.name,
                load = { upcomingProgrammes(target.tvgId) },
                onDismiss = { epgOpen = false }
            )
        }
    } else {
        MobilePlaybackOverlay(
            channel = target,
            title = target.name,
            isLive = target.kind == "live",
            resolveUrl = resolve,
            loadResumeMs = loadResume,
            saveResumeMs = saveResume,
            onClose = onClose,
            fetchSubtitles = { engine -> SubtitleWiring.autoFetch(ctx, target, engine) },
            searchSubtitles = { query -> SubtitleWiring.search(ctx, target, query) },
            applySubtitle = { engine, choice -> SubtitleWiring.apply(ctx, engine, choice) },
            // Ο τίτλος κάθεται πλέον ΚΑΤΩ από την εικόνα, όπως στο YouTube, οπότε
            // υπάρχει χώρος και για δεύτερη γραμμή. Στα ζωντανά δεν έχει έτος ή
            // είδος — μένει κενή αντί να γράψει παύλες.
            subtitle = listOf(target.year, target.genre)
                .filter(String::isNotBlank).joinToString(" · "),
            nextTitle = nextEpisode?.name,
            nextImageUrl = nextImageUrl,
            onPlayNext = nextEpisode?.let { next -> { onPlayOther(next) } },
            onChannelStep = channelStep,
            infoChannel = infoChannel,
            metadata = playerMetadata,
            relatedItems = contextItems,
            seasons = if (target.kind == "series_ep" || infoChannel.kind == "series") seasons else emptyList(),
            onPlayContextItem = { item ->
                if (item.kind == "series") onOpenDetails(item) else onPlayOther(item)
            },
            modifier = playerModifier
        )
    }
}
