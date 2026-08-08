@file:android.annotation.SuppressLint("ProduceStateDoesNotAssignValue")

package com.prelude.iptv.ui.components.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.LibraryDestination

@Immutable
data class PremiumLibraryContent(
    val myList: List<Channel> = emptyList(),
    val continueWatching: List<Channel> = emptyList(),
    val history: List<Channel> = emptyList(),
    val progress: Map<String, Float> = emptyMap()
) {
    val all: List<Channel>
        get() = libraryUnique(continueWatching + myList + history)
}

enum class LibraryHubTab(val label: String, val destination: LibraryDestination?) {
    ALL("Όλα", null),
    MY_LIST("Η λίστα μου", LibraryDestination.MY_LIST),
    CONTINUE("Συνέχεια", LibraryDestination.CONTINUE_WATCHING),
    HISTORY("Ιστορικό", LibraryDestination.HISTORY)
}

enum class LibrarySort(val label: String) {
    RECENT("Πρόσφατα"),
    TITLE("Τίτλος")
}

@Immutable
data class LibraryRail(
    val id: String,
    val title: String,
    val subtitle: String,
    val destination: LibraryDestination,
    val items: List<Channel>,
    val poster: Boolean
)

fun libraryRails(
    content: PremiumLibraryContent,
    tab: LibraryHubTab,
    sort: LibrarySort
): List<LibraryRail> {
    fun sorted(items: List<Channel>): List<Channel> = when (sort) {
        LibrarySort.RECENT -> items
        LibrarySort.TITLE -> items.sortedBy { it.name.lowercase() }
    }
    return when (tab) {
        LibraryHubTab.ALL -> listOf(
            LibraryRail("continue", "Συνέχισε να βλέπεις", "Από εκεί που σταμάτησες", LibraryDestination.CONTINUE_WATCHING, sorted(content.continueWatching), false),
            LibraryRail("my-list", "Η λίστα μου", "Οι αποθηκευμένες επιλογές σου", LibraryDestination.MY_LIST, sorted(content.myList), true),
            LibraryRail("history", "Πρόσφατα προβληθέντα", "Το ιστορικό σου", LibraryDestination.HISTORY, sorted(content.history), false)
        ).filter { it.items.isNotEmpty() }
        LibraryHubTab.MY_LIST -> listOf(
            LibraryRail("my-list", "Η λίστα μου", "${content.myList.size} αποθηκευμένα", LibraryDestination.MY_LIST, sorted(content.myList), true)
        ).filter { it.items.isNotEmpty() }
        LibraryHubTab.CONTINUE -> listOf(
            LibraryRail("continue", "Συνέχισε να βλέπεις", "${content.continueWatching.size} σε εξέλιξη", LibraryDestination.CONTINUE_WATCHING, sorted(content.continueWatching), false)
        ).filter { it.items.isNotEmpty() }
        LibraryHubTab.HISTORY -> listOf(
            LibraryRail("history", "Πρόσφατα προβληθέντα", "${content.history.size} πρόσφατα", LibraryDestination.HISTORY, sorted(content.history), false)
        ).filter { it.items.isNotEmpty() }
    }
}

fun initialLibraryTab(destination: LibraryDestination): LibraryHubTab = when (destination) {
    LibraryDestination.MY_LIST -> LibraryHubTab.MY_LIST
    LibraryDestination.CONTINUE_WATCHING -> LibraryHubTab.CONTINUE
    LibraryDestination.HISTORY -> LibraryHubTab.HISTORY
    LibraryDestination.SEARCH -> LibraryHubTab.ALL
}

fun libraryKey(channel: Channel): String = PlaybackQueue.favKey(channel)

fun libraryProgress(channel: Channel, content: PremiumLibraryContent): Float? =
    content.progress[libraryKey(channel)]?.coerceIn(0f, 1f)

fun libraryMetaLine(channel: Channel, meta: TmdbClient.Meta? = null): String = listOf(
    meta?.year?.takeIf(String::isNotBlank) ?: channel.year,
    meta?.genres?.takeIf(String::isNotBlank) ?: channel.genre.ifBlank { channel.group },
    channel.duration
).filter { it.isNotBlank() }.joinToString("  ·  ")

fun libraryTitle(channel: Channel): String =
    TmdbClient.cleanTitle(channel.name).ifBlank { channel.name }

fun libraryDescription(channel: Channel, meta: TmdbClient.Meta?): String =
    meta?.overview?.takeIf(String::isNotBlank)
        ?: channel.plot.takeIf(String::isNotBlank)
        ?: "Άνοιξε τον τίτλο για περισσότερες πληροφορίες και επιλογές αναπαραγωγής."

fun libraryUnique(items: Iterable<Channel>): List<Channel> {
    val seen = HashSet<String>()
    return items.filter { seen.add(libraryKey(it).ifBlank { "${it.kind}|${it.name}|${it.url}" }) }
}

@Composable
fun rememberLibraryMeta(
    channel: Channel?,
    tmdbFor: suspend (Channel) -> TmdbClient.Meta?
): State<TmdbClient.Meta?> = produceState<TmdbClient.Meta?>(null, channel) {
    value = channel?.let { tmdbFor(it) }
}
