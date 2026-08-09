package com.prelude.iptv.ui.coordinator

import android.app.Application
import com.prelude.iptv.data.PlaylistStore
import com.prelude.iptv.tvhome.TvHomeSyncScheduler
import com.prelude.iptv.ui.LoadPolicy
import com.prelude.iptv.ui.UiState
import com.prelude.iptv.ui.profile.ProfileDisplayName
import com.prelude.iptv.ui.profile.ProfilePresentationPolicy
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Owns profile selection and parental-control state.
 *
 * MainViewModel remains the public API boundary and keeps the catalog/player
 * concerns; this coordinator isolates the synchronous, source-independent
 * profile and parental logic so it no longer mixes with source loading.
 *
 * No coroutines, no provider access and no `View` ownership: every operation is
 * a deterministic mutation of [store], [profiles] and [state], which keeps the
 * behaviour identical to the previous inline implementation and unit-testable.
 */
internal class ProfileSettingsCoordinator(
    private val app: Application,
    private val store: PlaylistStore,
    private val state: MutableStateFlow<UiState>,
    private val profiles: MutableStateFlow<List<PlaylistStore.Profile>>,
) {
    /** Πότε δόθηκε το PIN. Το ξεκλείδωμα ΛΗΓΕΙ — αλλιώς ξεκλείδωνες το βράδυ
     *  και το παιδί έβρισκε την εφαρμογή ξεκλείδωτη το πρωί. */
    private var unlockedAtMs = 0L
    private val unlockTtlMs = 30 * 60 * 1000L

    /* ==================== Προφίλ ==================== */

    fun profiles(): List<PlaylistStore.Profile> = profiles.value

    fun activeProfileId(): Int = store.activeProfile

    fun activeProfileName(): String =
        store.profiles().firstOrNull { it.id == store.activeProfile }?.name
            ?: PlaylistStore.LEGACY_PRIMARY_PROFILE_NAME

    fun activeProfileDisplayName(): ProfileDisplayName =
        store.profiles().firstOrNull { it.id == store.activeProfile }
            ?.let(ProfilePresentationPolicy::displayName)
            ?: ProfileDisplayName.Primary

    /**
     * Χρειάζεται PIN για να ΜΠΕΙΣ σε αυτό το προφίλ;
     * Χωρίς αυτό, το παιδί θα άλλαζε απλώς προφίλ και θα παρέκαμπτε κάθε
     * κλείδωμα — ο γονικός έλεγχος θα ήταν διακοσμητικός.
     */
    fun profileNeedsPin(p: PlaylistStore.Profile): Boolean =
        p.protected && hasParentalPin() && p.id != store.activeProfile

    fun addProfile(name: String, protectedProfile: Boolean) {
        val list = store.profiles()
        val id = (list.maxOfOrNull { it.id } ?: 0) + 1
        list.add(
            PlaylistStore.Profile(
                id, name.trim().ifBlank { PlaylistStore.legacyGeneratedProfileName(id) }, protectedProfile
            )
        )
        store.saveProfiles(list)
        profiles.value = list.toList()
    }

    fun deleteProfile(id: Int) {
        if (id == 0) return                   // το βασικό δεν διαγράφεται
        val remaining = store.profiles().filterNot { it.id == id }
        store.saveProfiles(remaining)
        profiles.value = remaining
        store.wipeProfile(id)                 // μην αφήνεις ορφανά κλειδιά
        if (store.activeProfile == id) store.activeProfile = 0
        TvHomeSyncScheduler.schedule(app)
    }

    /** Η αλλαγή προφίλ γίνεται με ΕΠΑΝΕΚΚΙΝΗΣΗ: store/ViewModel διαβάζουν τα
     *  κλειδιά στο init — αλλιώς θα ανακατεύονταν δεδομένα δύο προφίλ στη μνήμη. */
    fun setActiveProfile(id: Int) {
        store.activeProfile = id
        TvHomeSyncScheduler.schedule(app)
    }

    /* ---- Γονικός έλεγχος ---- */

    fun hasParentalPin(): Boolean = store.hasParentalPin()

    fun checkPin(pin: String): Boolean = store.verifyParentalPin(pin)

    fun setParentalPin(pin: String) {
        store.setParentalPin(pin)
        // αλλαγή PIN ξανακλειδώνει τη συνεδρία — αλλιώς το παλιό ξεκλείδωμα μένει
        state.value = state.value.copy(parentalUnlocked = false)
    }

    fun unlockParental(pin: String): Boolean {
        if (!checkPin(pin)) return false
        unlockedAtMs = System.currentTimeMillis()
        state.value = state.value.copy(parentalUnlocked = true)
        return true
    }

    /** Λήξη ξεκλειδώματος — ελέγχεται σε κάθε επιστροφή στην εφαρμογή. */
    fun expireParentalIfNeeded() {
        if (state.value.parentalUnlocked &&
            LoadPolicy.isUnlockExpired(unlockedAtMs, System.currentTimeMillis(), unlockTtlMs)
        ) state.value = state.value.copy(parentalUnlocked = false)
    }

    fun toggleLockGroup(g: String) {
        val set = store.lockedGroups()
        val nowLocked = set.add(g)
        if (!nowLocked) set.remove(g)
        store.saveLockedGroups(set)
        TvHomeSyncScheduler.schedule(app)
        // Κλείδωσες το group που ΒΛΕΠΕΙΣ; Τότε μένει επιλεγμένο ενώ το φίλτρο
        // το κρύβει -> κενή οθόνη με μήνυμα «τίποτα δεν ταιριάζει» = αδιέξοδο.
        // Γυρνάμε στα «Όλα», που είναι και το προφανές επόμενο βήμα.
        val jumpOut = nowLocked && state.value.selectedGroup == g && !state.value.parentalUnlocked
        state.value = state.value.copy(
            lockedGroups = set,
            selectedGroup = if (jumpOut) UiState.ALL_GROUP else state.value.selectedGroup
        )
    }
}
