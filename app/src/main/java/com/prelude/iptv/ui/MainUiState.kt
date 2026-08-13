package com.prelude.iptv.ui

import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.SourceLoadProgress
import com.prelude.iptv.ui.epg.EpgSourceOption
import com.prelude.iptv.ui.epg.EpgStatus

data class CategoryPickerSection(
    val categories: List<Pair<String, String>> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    /** null means that every non-empty category is visible. */
    val selectedIds: Set<String>? = null,
)

data class UiState(
    val playlists: List<Playlist> = emptyList(),
    val currentIndex: Int = 0,
    val contentType: String = "live",
    val channels: List<Channel> = emptyList(),
    val groups: List<String> = emptyList(),
    val selectedGroup: String = ALL_GROUP,
    val search: String = "",
    val loading: Boolean = false,
    /** Background import triggered by the “Όλα” section choice. */
    val loadingAllSections: Boolean = false,
    /** Sections already available while the remaining sections continue in background. */
    val loadedSections: Set<String> = emptySet(),
    /** Transient progress keyed by stable source id; never persisted. */
    val sourceProgress: Map<String, SourceLoadProgress> = emptyMap(),
    /** Ανεβαίνει σε κάθε ON_RESUME: αναγκάζει το «Συνέχισε να βλέπεις» να
     *  ξαναδιαβάσει θέσεις/recents που άλλαξαν μέσα στον player. */
    val recentsVersion: Int = 0,
    /** Γονικός έλεγχος: κλειδωμένα groups (ονόματα) + αν δόθηκε PIN στη συνεδρία. */
    val lockedGroups: Set<String> = emptySet(),
    val parentalUnlocked: Boolean = false,
    /** Ταξινόμηση προβολής: default / az / za / year. Ανά συνεδρία. */
    val sortMode: String = "default",
    val status: String = "",
    val favorites: Set<String> = emptySet(),
    // επιλογή τύπου περιεχομένου στην αρχή (Xtream)
    val chooseContent: Boolean = false,
    // relay (MAC → M3U)
    val relayRunning: Boolean = false,
    val relayUrl: String = "",
    val epgLoaded: Boolean = false,
    val epgSources: List<EpgSourceOption> = emptyList(),
    val epgStatus: EpgStatus = EpgStatus.Idle,
    // τοπική επιλογή ορατών κατηγοριών μετά την πλήρη φόρτωση
    val pickCategories: Boolean = false,
    val categories: List<Pair<String, String>> = emptyList(),
    val categoryCounts: Map<String, Int> = emptyMap(),
    /** null = όλα επιλεγμένα, set = προεπιλεγμένα ids από την προηγούμενη επιλογή. */
    val categorySelectionIds: Set<String>? = null,
    /** Complete local category metadata for the Series / Movies / Live tabs. */
    val categoryPickerSections: Map<String, CategoryPickerSection> = emptyMap(),
    val categoryPickerType: String = "series",
    /** ενότητα που ζητήθηκε αλλά δεν είναι φορτωμένη -> «να τη φορτώσω;» */
    val askLoadType: String? = null,
    val fontScale: Float = 1.0f,
    // σειρές (Xtream drill-down)
    val openSeriesTitle: String? = null,
    val seriesSeasons: List<Pair<String, List<Channel>>> = emptyList(),
    val seriesLoading: Boolean = false
) {
    companion object {
        const val ALL_GROUP = "Όλα τα κανάλια"
        const val FAV_GROUP = "★ Αγαπημένα"
    }
}
