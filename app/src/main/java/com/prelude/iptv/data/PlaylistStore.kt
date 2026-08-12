package com.prelude.iptv.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.prelude.iptv.category.CategoryLayout

/** Αποθηκεύει playlists + αγαπημένα + volume σε SharedPreferences. */
class PlaylistStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val secure = SecureStorage(context.applicationContext)
    private val playbackHistory by lazy {
        PlaybackHistoryStore(
            prefs = prefs,
            profileKey = ::pk,
            loadChannels = ::loadChannelArray,
            saveChannels = ::saveChannelArray,
            removeSecure = secure::remove
        )
    }

    companion object {
        /** ΕΝΑ σημείο αλήθειας για το όνομα των prefs (το χρησιμοποιεί και το Backup). */
        const val PREFS = "upl_prefs"

        /** Existing synthesized value; kept stable for persisted-profile and backup compatibility. */
        const val LEGACY_PRIMARY_PROFILE_NAME = "Κύριος"

        fun legacyGeneratedProfileName(id: Int): String = "Προφίλ $id"
    }

    init {
        migrateSensitivePreferences()
    }

    /* ==================== Προφίλ ====================
       ΣΧΕΔΙΑΣΜΟΣ (γιατί έτσι):
       - ΚΟΙΝΑ σε όλα τα προφίλ: λίστες, επιλογές κατηγοριών, API keys, master
         PIN. Ανήκουν στη ΣΥΣΚΕΥΗ/συνδρομή — δεν έχει νόημα να ξαναπροσθέτει
         ο καθένας την ίδια λίστα.
       - ΑΝΑ ΠΡΟΦΙΛ: αγαπημένα, πρόσφατα, θέσεις, κλειδωμένα groups,
         γραμματοσειρά. Ανήκουν στο ΑΤΟΜΟ.
       - Το προφίλ 0 γράφει στα ΑΡΧΙΚΑ κλειδιά (χωρίς prefix): όποιος
         αναβαθμίσει δεν χάνει αγαπημένα/ιστορικό. Τα υπόλοιπα παίρνουν "pN_".
       - Προφίλ με protected=true θέλει master PIN για να μπεις — αλλιώς το
         παιδί θα άλλαζε απλώς προφίλ και θα παρέκαμπτε κάθε κλείδωμα. */

    data class Profile(val id: Int, val name: String, val protected: Boolean)

    var activeProfile: Int
        get() = prefs.getInt("active_profile", 0)
        set(v) { prefs.edit().putInt("active_profile", v).apply() }

    fun profiles(): MutableList<Profile> {
        val raw = prefs.getString("profiles", "") ?: ""
        if (raw.isBlank()) return mutableListOf(Profile(0, LEGACY_PRIMARY_PROFILE_NAME, false))
        val arr = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        val out = ArrayList<Profile>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(Profile(o.optInt("id"), o.optString("name"), o.optBoolean("protected")))
        }
        return if (out.isEmpty()) mutableListOf(Profile(0, LEGACY_PRIMARY_PROFILE_NAME, false)) else out
    }

    fun saveProfiles(list: List<Profile>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("protected", it.protected))
        }
        prefs.edit().putString("profiles", arr.toString()).apply()
    }

    /** Κλειδί που ανήκει στο ΤΡΕΧΟΝ προφίλ. Προφίλ 0 = αρχικά κλειδιά. */
    private fun pk(key: String): String {
        val p = activeProfile
        return if (p == 0) key else "p${p}_$key"
    }

    /** Σβήνει ό,τι ανήκει σε ένα προφίλ (διαγραφή προφίλ). */
    fun wipeProfile(id: Int) {
        if (id == 0) return                       // το βασικό δεν διαγράφεται
        val pre = "p${id}_"
        val ed = prefs.edit()
        prefs.all.keys.filter { it.startsWith(pre) }.forEach { ed.remove(it) }
        ed.apply()
        secure.keys().filter { it.startsWith(pre) }.forEach(secure::remove)
    }

    fun loadPlaylists(): MutableList<Playlist> {
        val raw = secure.getString("playlists") ?: prefs.getString("playlists", null)?.also { legacy ->
            secure.putString("playlists", legacy)
            prefs.edit().remove("playlists").apply()
        } ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        val out = ArrayList<Playlist>()
        for (i in 0 until arr.length()) {
            runCatching { out.add(Playlist.fromJson(arr.getJSONObject(i))) }
        }
        return out
    }

    fun savePlaylists(list: List<Playlist>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        secure.putString("playlists", arr.toString())
        prefs.edit().remove("playlists").apply()
    }

    fun loadFavorites(): MutableSet<String> {
        val key = pk("favorites")
        secure.getString(key)?.let { raw ->
            val arr = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
            return (0 until arr.length()).mapNotNull { arr.optString(it).takeIf(String::isNotBlank) }.toMutableSet()
        }
        val legacy = (prefs.getStringSet(key, emptySet()) ?: emptySet()).toMutableSet()
        if (legacy.isNotEmpty()) saveFavorites(legacy) else prefs.edit().remove(key).apply()
        return legacy
    }

    fun saveFavorites(favs: Set<String>) {
        val key = pk("favorites")
        secure.putString(key, JSONArray(favs.toList()).toString())
        prefs.edit().remove(key).apply()
    }

    /**
     * Snapshot metadata for favorites. The old implementation persisted only an
     * opaque key, so "My List" could not reconstruct an item after switching
     * section or restarting the app. Keys remain the source of truth; snapshots
     * only provide the title/artwork/playback fields needed by the library UI.
     */
    fun loadFavoriteItems(): MutableList<Channel> = loadChannelArray(pk("favorite_items"))

    fun addFavoriteItem(channel: Channel) {
        val key = PlaybackQueue.favKey(channel)
        if (key.isBlank()) return
        val list = loadFavoriteItems()
        list.removeAll { PlaybackQueue.favKey(it) == key }
        list.add(0, channel)
        saveChannelArray(pk("favorite_items"), list.take(100))
    }

    fun removeFavoriteItem(key: String) {
        if (key.isBlank()) return
        val list = loadFavoriteItems()
        if (list.removeAll { PlaybackQueue.favKey(it) == key }) {
            saveChannelArray(pk("favorite_items"), list)
        }
    }

    /* ---- Source-scoped favorites v2 ------------------------------------
       The legacy store above is intentionally kept read-only for migration.
       New writes include the stable source identity so identical URLs/IDs from
       two providers can never route a favorite to the wrong account. */

    private fun sourceFavoritesKey(): String = pk("source_favorites_v2")
    private fun sourceFavoriteMigrationKey(sourceId: String): String =
        pk("source_favorites_migrated_${PlaylistIdentity.digest(sourceId)}")

    fun loadSourceFavorites(): MutableList<SourceFavorite> {
        val raw = secure.getString(sourceFavoritesKey()) ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return buildList {
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val sourceId = obj.optString("sourceId")
                val itemKey = obj.optString("itemKey")
                val channel = obj.optJSONObject("channel")?.let(::sourceFavoriteChannelFromJson) ?: continue
                if (sourceId.isBlank() || itemKey.isBlank()) continue
                add(
                    SourceFavorite(
                        sourceId = sourceId,
                        itemKey = itemKey,
                        channel = channel,
                        addedAtMs = obj.optLong("addedAtMs", 0L)
                    )
                )
            }
        }.distinctBy { it.identity }.toMutableList()
    }

    private fun saveSourceFavorites(entries: List<SourceFavorite>) {
        val array = JSONArray()
        entries.distinctBy { it.identity }.take(500).forEach { entry ->
            array.put(
                JSONObject()
                    .put("sourceId", entry.sourceId)
                    .put("itemKey", entry.itemKey)
                    .put("addedAtMs", entry.addedAtMs)
                    .put("channel", sourceFavoriteChannelToJson(entry.channel))
            )
        }
        secure.putString(sourceFavoritesKey(), array.toString())
    }

    /** Favorite keys visible for one provider only. */
    fun loadFavorites(sourceId: String): MutableSet<String> =
        if (sourceId.isBlank()) mutableSetOf()
        else loadSourceFavorites().asSequence()
            .filter { it.sourceId == sourceId }
            .map { it.itemKey }
            .toMutableSet()

    fun loadFavoriteItems(sourceId: String): MutableList<Channel> =
        if (sourceId.isBlank()) mutableListOf()
        else loadSourceFavorites().asSequence()
            .filter { it.sourceId == sourceId }
            .sortedByDescending { it.addedAtMs }
            .map { it.channel }
            .toMutableList()

    fun isFavorite(sourceId: String, itemKey: String): Boolean =
        sourceId.isNotBlank() && itemKey.isNotBlank() &&
            loadSourceFavorites().any { it.sourceId == sourceId && it.itemKey == itemKey }

    fun addFavoriteItem(sourceId: String, channel: Channel) {
        val itemKey = PlaybackQueue.favKey(channel)
        if (sourceId.isBlank() || itemKey.isBlank()) return
        val entries = loadSourceFavorites()
        entries.removeAll { it.sourceId == sourceId && it.itemKey == itemKey }
        entries.add(
            0,
            SourceFavorite(
                sourceId = sourceId,
                itemKey = itemKey,
                channel = channel,
                addedAtMs = System.currentTimeMillis()
            )
        )
        saveSourceFavorites(entries)
    }

    fun removeFavoriteItem(sourceId: String, itemKey: String) {
        if (sourceId.isBlank() || itemKey.isBlank()) return
        val entries = loadSourceFavorites()
        if (entries.removeAll { it.sourceId == sourceId && it.itemKey == itemKey }) {
            saveSourceFavorites(entries)
        }
    }

    fun clearFavorites(sourceId: String) {
        if (sourceId.isBlank()) return
        val entries = loadSourceFavorites()
        if (entries.removeAll { it.sourceId == sourceId }) saveSourceFavorites(entries)
    }

    /**
     * Updates stored snapshots from a fresh provider catalog and migrates old
     * unscoped favorites only on an exact item-key match. Ambiguous rows remain
     * in the legacy store instead of being guessed into the wrong provider.
     */
    fun reconcileFavorites(sourceId: String, sourceItems: List<Channel>) {
        if (sourceId.isBlank() || sourceItems.isEmpty()) return
        var entries = SourceFavoritePolicy.reconcileSnapshots(
            loadSourceFavorites(),
            sourceId,
            sourceItems
        ).toMutableList()
        val existing = entries.map { it.identity }.toHashSet()
        val migrated = (prefs.getStringSet(sourceFavoriteMigrationKey(sourceId), emptySet()) ?: emptySet()).toMutableSet()
        val legacyKeys = buildSet {
            addAll(loadFavorites())
            loadFavoriteItems().mapTo(this) { PlaybackQueue.favKey(it) }
        }.filter(String::isNotBlank).toSet()
        val matches = SourceFavoritePolicy.selectLegacyMatches(
            sourceId = sourceId,
            legacyKeys = legacyKeys,
            sourceItems = sourceItems,
            nowMs = System.currentTimeMillis()
        )
        matches.forEach { match ->
            val marker = PlaylistIdentity.digest(match.identity)
            if (marker !in migrated) {
                migrated += marker
                if (existing.add(match.identity)) entries.add(match)
            }
        }
        saveSourceFavorites(entries)
        prefs.edit().putStringSet(sourceFavoriteMigrationKey(sourceId), migrated).apply()
    }

    private fun sourceFavoriteChannelToJson(channel: Channel): JSONObject = JSONObject()
        .put("name", channel.name).put("group", channel.group).put("logo", channel.logo)
        .put("tvgId", channel.tvgId).put("url", channel.url).put("cmd", channel.cmd)
        .put("chId", channel.chId).put("streamId", channel.streamId).put("kind", channel.kind)
        .put("seriesId", channel.seriesId).put("plot", channel.plot).put("cast", channel.cast)
        .put("director", channel.director).put("genre", channel.genre).put("year", channel.year)
        .put("duration", channel.duration)

    private fun sourceFavoriteChannelFromJson(obj: JSONObject): Channel = Channel(
        name = obj.optString("name"), group = obj.optString("group"), logo = obj.optString("logo"),
        tvgId = obj.optString("tvgId"), url = obj.optString("url"), cmd = obj.optString("cmd"),
        chId = obj.optString("chId"), streamId = obj.optString("streamId"), kind = obj.optString("kind", "live"),
        seriesId = obj.optString("seriesId"), plot = obj.optString("plot"), cast = obj.optString("cast"),
        director = obj.optString("director"), genre = obj.optString("genre"), year = obj.optString("year"),
        duration = obj.optString("duration")
    )

    private fun loadChannelArray(prefKey: String): MutableList<Channel> {
        val raw = secure.getString(prefKey) ?: prefs.getString(prefKey, null)?.also { legacy ->
            secure.putString(prefKey, legacy)
            prefs.edit().remove(prefKey).apply()
        } ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        val out = ArrayList<Channel>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            runCatching {
                out += Channel(
                    name = o.optString("name"), logo = o.optString("logo"),
                    url = o.optString("url"), cmd = o.optString("cmd"),
                    kind = o.optString("kind", "live"), seriesId = o.optString("seriesId"),
                    group = o.optString("group"), tvgId = o.optString("tvgId"),
                    year = o.optString("year"), plot = o.optString("plot"),
                    cast = o.optString("cast"), director = o.optString("director"),
                    genre = o.optString("genre"), duration = o.optString("duration"),
                    streamId = o.optString("streamId"), chId = o.optString("chId")
                )
            }
        }
        return out
    }

    private fun saveChannelArray(prefKey: String, list: List<Channel>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("name", it.name).put("logo", it.logo).put("url", it.url)
                    .put("cmd", it.cmd).put("kind", it.kind).put("seriesId", it.seriesId)
                    .put("group", it.group).put("tvgId", it.tvgId)
                    .put("year", it.year).put("plot", it.plot)
                    .put("cast", it.cast).put("director", it.director)
                    .put("genre", it.genre).put("duration", it.duration)
                    .put("streamId", it.streamId).put("chId", it.chId)
            )
        }
        secure.putString(prefKey, arr.toString())
        prefs.edit().remove(prefKey).apply()
    }

    /* ---- Γονικός έλεγχος ----
       Τα κλειδωμένα groups αποθηκεύονται με ΟΝΟΜΑ, καθολικά: κλειδώνεις το
       «ADULTS» μία φορά και ισχύει σε ΟΛΕΣ τις λίστες — αυτό θέλει ο γονιός,
       όχι να το ξανακλειδώνει ανά λίστα/πάροχο. */
    fun hasParentalPin(): Boolean = parentalPinHash().isNotBlank()

    fun verifyParentalPin(pin: String): Boolean =
        pin.isNotBlank() && PinHasher.verify(pin, parentalPinHash())

    fun setParentalPin(pin: String) {
        if (pin.isBlank()) secure.remove("pp_pin") else secure.putString("pp_pin", PinHasher.hash(pin))
        prefs.edit().remove("pp_pin").apply()
    }

    private fun parentalPinHash(): String {
        secure.getString("pp_pin")?.let { return it }
        val legacy = prefs.getString("pp_pin", "") ?: ""
        if (legacy.isBlank()) return ""
        val migrated = if (PinHasher.isHash(legacy)) legacy else PinHasher.hash(legacy)
        secure.putString("pp_pin", migrated)
        prefs.edit().remove("pp_pin").apply()
        return migrated
    }

    fun lockedGroups(): MutableSet<String> =
        (prefs.getStringSet(pk("pp_locked"), emptySet()) ?: emptySet()).toMutableSet()

    fun saveLockedGroups(g: Set<String>) {
        prefs.edit().putStringSet(pk("pp_locked"), g).apply()
    }

    var lastPlaylist: Int
        get() = prefs.getInt(pk("last_playlist"), 0)
        set(v) = prefs.edit().putInt(pk("last_playlist"), v).apply()

    var fontScale: Float
        get() = prefs.getFloat(pk("font_scale"), 1.0f)
        set(v) = prefs.edit().putFloat(pk("font_scale"), v).apply()

    /** auto | exo | vlc */
    var playerMode: String
        get() = prefs.getString("player_mode", "auto") ?: "auto"
        set(v) = prefs.edit().putString("player_mode", v).apply()

    /** low | normal | high — πόσο απόθεμα εικόνας κρατά ο player. */
    var bufferProfile: String
        get() = prefs.getString("buffer_profile", "normal") ?: "normal"
        set(v) = prefs.edit().putString(
            "buffer_profile",
            when (v) {
                "low", "high" -> v
                else -> "normal"
            }
        ).apply()

    /**
     * off | seamless | always — καθολικό για τη συσκευή/τηλεόραση.
     *
     * ΕΞ ΟΡΙΣΜΟΥ «seamless», όχι «off». Χωρίς αντιστοίχιση συχνότητας, μια
     * μετάδοση 50 καρέ σε οθόνη 60 Hz δείχνει κάθε πέμπτο καρέ διπλή ώρα — η
     * κίνηση σπάει ορατά σε αθλητικά. Η επιλογή «seamless» ζητά απλώς συχνότητα
     * και δεν επιβάλλει αλλαγή λειτουργίας HDMI, οπότε δεν μαυρίζει ποτέ την
     * οθόνη: δεν υπάρχει λόγος να είναι κλειστή από μόνη της. Όποιος τη θέλει
     * σβηστή μπορεί να τη σβήσει.
     */
    var autoFrameRateMode: String
        get() = prefs.getString("auto_frame_rate_mode", "seamless") ?: "seamless"
        set(v) = prefs.edit().putString(
            "auto_frame_rate_mode",
            when (v) {
                "seamless", "always" -> v
                else -> "off"
            }
        ).apply()

    /** Publishes source-scoped Continue Watching items to the Android TV launcher. */
    var tvHomeEnabled: Boolean
        get() = prefs.getBoolean("tv_home_enabled", false)
        set(v) = prefs.edit().putBoolean("tv_home_enabled", v).apply()

    /** Publishes source-scoped favorites as an app preview channel on Android TV. */
    var tvHomeMyListEnabled: Boolean
        get() = prefs.getBoolean("tv_home_my_list_enabled", false)
        set(v) = prefs.edit().putBoolean("tv_home_my_list_enabled", v).apply()

    /** Opens the last active source automatically when the application starts. */
    var autoOpenPlaylist: Boolean
        get() = prefs.getBoolean("auto_open_playlist", true)
        set(v) = prefs.edit().putBoolean("auto_open_playlist", v).apply()

    /** User-facing source refresh preference, stored in days. */
    var playlistRefreshDays: Int
        get() = PlaylistPreferencePolicy.normalizeRefreshDays(prefs.getInt("playlist_refresh_days", 3))
        set(v) = prefs.edit().putInt("playlist_refresh_days", PlaylistPreferencePolicy.normalizeRefreshDays(v)).apply()

    /** Μέγεθος υποτίτλων Player, αποθηκευμένο ανά προφίλ. */
    var subtitleSizePercent: Int
        get() = prefs.getInt(pk("subtitle_size_percent"), 100).coerceIn(70, 180)
        set(v) = prefs.edit().putInt(pk("subtitle_size_percent"), v.coerceIn(70, 180)).apply()

    /** Shared caption weight for embedded and downloaded subtitles. */
    var subtitleBold: Boolean
        get() = prefs.getBoolean(pk("subtitle_bold"), false)
        set(v) = prefs.edit().putBoolean(pk("subtitle_bold"), v).apply()

    var startWithSubtitles: Boolean
        get() = prefs.getBoolean(pk("start_with_subtitles"), true)
        set(v) = prefs.edit().putBoolean(pk("start_with_subtitles"), v).apply()

    var preferredSubtitleLanguage: String
        get() = PlaybackPreferencePolicy.normalizeLanguage(prefs.getString(pk("preferred_subtitle_language"), ""))
        set(v) = prefs.edit().putString(pk("preferred_subtitle_language"), PlaybackPreferencePolicy.normalizeLanguage(v)).apply()

    var preferredAudioLanguage: String
        get() = PlaybackPreferencePolicy.normalizeLanguage(prefs.getString(pk("preferred_audio_language"), ""))
        set(v) = prefs.edit().putString(pk("preferred_audio_language"), PlaybackPreferencePolicy.normalizeLanguage(v)).apply()

    /**
     * Φόντο υποτίτλων: "none" | "shadow" | "box".
     *
     * Προεπιλογή η σκιά. Το πλαίσιο διαβάζεται καλύτερα σε φωτεινές σκηνές αλλά
     * κρύβει περισσότερη εικόνα — γι' αυτό είναι επιλογή και όχι απόφαση δική μας.
     */
    var subtitleBackground: String
        get() = prefs.getString(pk("subtitle_background"), "shadow") ?: "shadow"
        set(v) = prefs.edit().putString(
            pk("subtitle_background"),
            when (v) {
                "none", "box" -> v
                else -> "shadow"
            }
        ).apply()

    // ---- διάταξη αρχικής κινητού («Επεξεργασία αρχικής») ----
    //
    // Ανά προφίλ, όχι ανά πηγή: η σειρά που θέλει κάποιος να βλέπει την αρχική
    // του είναι προτίμηση του ίδιου, όχι της λίστας που τυχαίνει να έχει ανοιχτή.
    // Αλλάζοντας πηγή δεν έχει νόημα να ξαναστήνει τα πάντα.
    //
    // Αποθηκεύεται ως κείμενο χωρισμένο με «|», όχι JSON: είναι μια λίστα από
    // σταθερά αναγνωριστικά χωρίς κενά, και το JSON εδώ θα ήταν τελετουργικό.

    /**
     * ΞΕΧΩΡΙΣΤΗ ΔΙΑΤΑΞΗ ΑΝΑ ΠΡΟΟΡΙΣΜΟ.
     *
     * Ένας επεξεργαστής κυβερνούσε τέσσερις οθόνες — Αρχική, Ζωντανά, Ταινίες,
     * Σειρές — και γι' αυτό η λίστα του δεν συμφωνούσε ποτέ με αυτό που έβλεπε ο
     * χρήστης: «Νέα επεισόδια» δεν μπορεί να υπάρξει στα Ζωντανά, «Νέα ζωντανά»
     * δεν μπορεί να υπάρξει στις Ταινίες.
     *
     * ΜΕΤΑΒΑΣΗ ΧΩΡΙΣ ΑΠΩΛΕΙΑ: ο προορισμός «home» κρατά τα ΑΡΧΙΚΑ κλειδιά
     * (`home_order`, `home_hidden`), οπότε η διάταξη που έχει ήδη φτιάξει ο
     * χρήστης γίνεται η διάταξη της Αρχικής χωρίς κώδικα μετανάστευσης. Οι άλλοι
     * τρεις ξεκινούν από τις προεπιλογές τους.
     */
    private fun sectionKey(base: String, destination: String): String =
        if (destination == "home") pk(base) else pk("${base}_$destination")

    fun homeSectionOrder(destination: String): List<String> =
        prefs.getString(sectionKey("home_order", destination), "").orEmpty()
            .split('|').filter { it.isNotBlank() }

    fun setHomeSectionOrder(destination: String, value: List<String>) {
        prefs.edit().putString(sectionKey("home_order", destination), value.joinToString("|")).apply()
    }

    fun homeHiddenSections(destination: String): Set<String> =
        prefs.getString(sectionKey("home_hidden", destination), "").orEmpty()
            .split('|').filter { it.isNotBlank() }.toSet()

    fun setHomeHiddenSections(destination: String, value: Set<String>) {
        prefs.edit().putString(sectionKey("home_hidden", destination), value.joinToString("|")).apply()
    }

    /**
     * Η κατηγορία που δείχνει ένα rail (π.χ. Ζωντανά → «DIGEA»).
     *
     * Κενό σημαίνει «διάλεξε εσύ» και ο κώδικας παίρνει τη μεγαλύτερη κατηγορία
     * της ενότητας — μια αρχική που ξεκινά άδεια επειδή δεν έχει ρυθμιστεί δεν
     * είναι ουδέτερη, είναι χαλασμένη.
     */
    fun homeRailCategory(sectionId: String): String =
        prefs.getString(pk("home_cat_$sectionId"), "").orEmpty()

    fun setHomeRailCategory(sectionId: String, group: String) {
        prefs.edit().putString(pk("home_cat_$sectionId"), group).apply()
    }

    /**
     * ΠΟΛΛΕΣ κατηγορίες ανά ενότητα: κάθε μία γίνεται δική της ράγα.
     *
     * Πριν, μια ενότητα έδειχνε ΜΙΑ κατηγορία, οπότε ο χρήστης δεν έφτιαχνε την
     * οθόνη του — διάλεγε ποια από τις εβδομήντα θα δει. Η σειρά της λίστας είναι
     * η σειρά των ραγών, γι' αυτό είναι `List` και όχι `Set`.
     *
     * ΣΥΜΒΑΤΟΤΗΤΑ: κενή λίστα σημαίνει «δεν έχει επιλέξει πολλαπλά», και τότε
     * ισχύει η παλιά μονή τιμή. Έτσι κανείς δεν χάνει τη ρύθμισή του και η
     * προεπιλογή «η μεγαλύτερη κατηγορία» εξακολουθεί να δουλεύει.
     *
     * Ο διαχωριστής είναι `\n`: τα ονόματα κατηγοριών του παρόχου περιέχουν
     * κάθετες («GR | KIDS | ΠΑΙΔΙΚΑ»), οπότε το `|` που χρησιμοποιείται αλλού θα
     * τα έκοβε στη μέση.
     */
    fun homeRailCategories(destination: String, sectionId: String): List<String> {
        val raw = prefs.getString(pk("home_cats_${destination}_$sectionId"), null)
        if (raw == null) return listOfNotNull(homeRailCategory(sectionId).takeIf { it.isNotBlank() })
        return raw.split('\n').filter { it.isNotBlank() }
    }

    fun setHomeRailCategories(destination: String, sectionId: String, groups: List<String>) {
        prefs.edit()
            .putString(pk("home_cats_${destination}_$sectionId"), groups.joinToString("\n"))
            .apply()
    }

    // ---- ρυθμίσεις υποτίτλων (OpenSubtitles) ----
    fun loadSubSettings(): Triple<String, String, String> = Triple(
        secureString("sub_key"),
        secureString("sub_user"),
        secureString("sub_pass")
    )

    // ---- ιστορικό «Συνέχισε» — απομονωμένο ανά πηγή ΚΑΙ ανά προφίλ ----

    fun loadRecents(sourceId: String): MutableList<Channel> =
        playbackHistory.loadRecents(sourceId)

    fun addRecent(sourceId: String, ch: Channel) =
        playbackHistory.addRecent(sourceId, ch)

    fun loadRecentLive(sourceId: String): MutableList<Channel> =
        playbackHistory.loadRecentLive(sourceId)

    fun addRecentLive(sourceId: String, ch: Channel) =
        playbackHistory.addRecentLive(sourceId, ch)

    fun clearRecentLive(sourceId: String) =
        playbackHistory.clearRecentLive(sourceId)

    fun removeRecent(sourceId: String, key: String) =
        playbackHistory.removeRecent(sourceId, key)

    fun migrateLegacyHistory(sourceId: String, sourceItems: List<Channel>) =
        playbackHistory.migrateLegacyHistory(sourceId, sourceItems)

    fun reconcileHistory(sourceId: String, sourceItems: List<Channel>) =
        playbackHistory.reconcileHistory(sourceId, sourceItems)

    /** If disabled, EPG is not downloaded. */

    var epgEnabled: Boolean
        get() = prefs.getBoolean("epg_enabled", true)
        set(v) = prefs.edit().putBoolean("epg_enabled", v).apply()

    // ---- τι επέλεξε ο χρήστης να φορτώνει (επιβιώνει σε επανεκκίνηση) ----

    /** ids = null σημαίνει «όλες οι κατηγορίες». */
    fun saveLoadChoice(key: String, ids: List<String>?) {
        prefs.edit().putString("lc_$key", ids?.joinToString("\u0001") ?: "*").apply()
    }

    /** @return (υπάρχει επιλογή, ids) — ids null = όλα */
    fun loadChoiceFor(key: String): Pair<Boolean, List<String>?> {
        val raw = prefs.getString("lc_$key", null) ?: return false to null
        if (raw == "*") return true to null
        return true to raw.split("\u0001").filter { it.isNotBlank() }
    }

    /** Ξεχνάει τις επιλογές μιας λίστας (π.χ. όταν διαγράφεται). */
    fun clearLoadChoices(plId: String) {
        val e = prefs.edit()
        prefs.all.keys.filter { it.startsWith("lc_$plId:") }.forEach { e.remove(it) }
        e.apply()
    }

    /** Category customisation is source-scoped and shared by all profiles. */
    fun loadCategoryLayout(sourceId: String, type: String): CategoryLayout {
        val raw = prefs.getString("category_layout_$sourceId:$type", null) ?: return CategoryLayout()
        return runCatching {
            val json = JSONObject(raw)
            fun strings(name: String): List<String> {
                val array = json.optJSONArray(name) ?: return emptyList()
                return buildList { for (i in 0 until array.length()) add(array.optString(i)) }
                    .filter { it.isNotBlank() }
            }
            CategoryLayout(
                order = strings("order"),
                orderedTitles = strings("orderedTitles"),
                hidden = strings("hidden").toSet(),
                deleted = strings("deleted").toSet(),
            )
        }.getOrDefault(CategoryLayout())
    }

    fun saveCategoryLayout(sourceId: String, type: String, layout: CategoryLayout) {
        val json = JSONObject()
            .put("order", JSONArray(layout.order))
            .put("orderedTitles", JSONArray(layout.orderedTitles))
            .put("hidden", JSONArray(layout.hidden.toList()))
            .put("deleted", JSONArray(layout.deleted.toList()))
        prefs.edit().putString("category_layout_$sourceId:$type", json.toString()).apply()
    }

    fun clearCategoryLayouts(sourceId: String) {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("category_layout_$sourceId:") }
            .forEach(editor::remove)
        editor.apply()
    }

    /** Τελευταία ενότητα (live/vod/series) ανά λίστα. */
    fun lastSection(plId: String): String = prefs.getString(pk("sec_$plId"), "live") ?: "live"
    fun saveLastSection(plId: String, t: String) {
        prefs.edit().putString(pk("sec_$plId"), t).apply()
    }

    /**
     * Ποια groups έχει διαλέξει ο χρήστης να προβάλλονται με μεγάλα rails στην
     * αρχική, ανά πηγή + ενότητα. Κρατάμε τη ΣΕΙΡΑ (join, όχι set). Κενή λίστα =
     * καμία επιλογή → ο caller πέφτει σε default (βλ. FeaturedGroupsPolicy).
     */
    fun loadFeaturedGroups(sourceId: String, type: String): List<String> {
        val raw = prefs.getString(pk("fg_$sourceId:$type"), null) ?: return emptyList()
        return raw.split("\n").filter { it.isNotBlank() }
    }

    fun saveFeaturedGroups(sourceId: String, type: String, groups: List<String>) {
        prefs.edit().putString(pk("fg_$sourceId:$type"), groups.joinToString("\n")).apply()
    }
    fun clearSection(plId: String) { prefs.edit().remove(pk("sec_$plId")).apply() }

    // ---- θέση αναπαραγωγής («συνέχισε από εκεί που έμεινες») ----

    /**
     * Αποθηκεύει τη θέση. Δεν κρατάει:
     *  - live (δεν έχει νόημα)
     *  - κάτω από 60 δευτ. (μόλις ξεκίνησες)
     *  - πάνω από 95% (ουσιαστικά τελείωσε → σβήνει)
     */
    data class SavedPosition(
        val positionMs: Long,
        val durationMs: Long,
        val lastEngagementMs: Long
    )

    fun savePosition(
        sourceId: String,
        key: String,
        positionMs: Long,
        durationMs: Long,
        lastEngagementMs: Long = System.currentTimeMillis()
    ) = playbackHistory.savePosition(
        sourceId = sourceId,
        itemKey = key,
        positionMs = positionMs,
        durationMs = durationMs,
        lastEngagementMs = lastEngagementMs
    )

    fun loadSavedPosition(sourceId: String, key: String): SavedPosition? =
        playbackHistory.loadSavedPosition(sourceId, key)?.let { saved ->
            SavedPosition(
                positionMs = saved.positionMs,
                durationMs = saved.durationMs,
                lastEngagementMs = saved.lastEngagementMs
            )
        }

    fun ensurePositionEngagement(sourceId: String, key: String, fallbackMs: Long): Long =
        playbackHistory.ensurePositionEngagement(sourceId, key, fallbackMs)

    /** @return (position, duration), or null when no resume point exists. */
    fun loadPosition(sourceId: String, key: String): Pair<Long, Long>? =
        loadSavedPosition(sourceId, key)?.let { it.positionMs to it.durationMs }

    fun clearPosition(sourceId: String, key: String) =
        playbackHistory.clearPosition(sourceId, key)

    fun clearHistory(sourceId: String) =
        playbackHistory.clearHistory(sourceId)

    /** TMDB API key (v3), used for ratings and artwork. */

    var tmdbKey: String
        get() = secureString("tmdb_key")
        set(v) {
            secure.putString("tmdb_key", v)
            prefs.edit().remove("tmdb_key").apply()
        }

    fun saveSubSettings(key: String, user: String, pass: String) {
        secure.putString("sub_key", key)
        secure.putString("sub_user", user)
        secure.putString("sub_pass", pass)
        prefs.edit().remove("sub_key").remove("sub_user").remove("sub_pass").apply()
    }

    /** Migrates old raw provider identities out of preference key names. */
    fun migrateLegacyPlaylistKeys(playlist: Playlist) {
        val legacyId = when (playlist.type) {
            PlaylistType.XTREAM -> "x|${playlist.server}|${playlist.username}"
            PlaylistType.STALKER -> "s|${playlist.portal}|${playlist.mac}"
            PlaylistType.M3U -> "m|${playlist.source}"
        }
        val stableId = PlaylistIdentity.stableId(playlist)
        if (legacyId == stableId) return
        val editor = prefs.edit()
        val snapshot = prefs.all.toMap()
        snapshot.forEach { (oldKey, value) ->
            val newKey = when {
                oldKey.startsWith("lc_$legacyId:") -> "lc_$stableId:${oldKey.substringAfter("lc_$legacyId:")}"
                oldKey.endsWith("sec_$legacyId") ->
                    oldKey.removeSuffix("sec_$legacyId") + "sec_$stableId"
                else -> null
            } ?: return@forEach
            if (!snapshot.containsKey(newKey)) putPreferenceValue(editor, newKey, value)
            editor.remove(oldKey)
        }
        editor.apply()
    }

    /** Removes obsolete preference keys that expose a URL, username or MAC in their name. */
    fun purgeUnsafeLegacyKeys() {
        val editor = prefs.edit()
        prefs.all.keys.filter { key ->
            key.contains("://") || key.contains("|00:", ignoreCase = true) ||
                key.startsWith("lc_x|") || key.startsWith("lc_s|") || key.startsWith("lc_m|") ||
                key.contains("_sec_x|") || key.contains("_sec_s|") || key.contains("_sec_m|") ||
                key.startsWith("sec_x|") || key.startsWith("sec_s|") || key.startsWith("sec_m|")
        }.forEach(editor::remove)
        editor.apply()
    }

    private fun migrateSensitivePreferences() {
        val editor = prefs.edit()
        prefs.all.toMap().forEach { (key, value) ->
            when {
                key == "pp_pin" && value is String && value.isNotBlank() -> {
                    val encoded = if (PinHasher.isHash(value)) value else runCatching { PinHasher.hash(value) }.getOrNull()
                    if (encoded != null) secure.putString(key, encoded)
                    editor.remove(key)
                }
                isSecureStringKey(key) && value is String -> {
                    secure.putString(key, value)
                    editor.remove(key)
                }
                isSecureSetKey(key) && value is Set<*> -> {
                    secure.putString(key, JSONArray(value.filterIsInstance<String>()).toString())
                    editor.remove(key)
                }
            }
        }
        editor.apply()
    }

    private fun isSecureStringKey(key: String): Boolean =
        key in setOf("playlists", "tmdb_key", "sub_key", "sub_user", "sub_pass") ||
            Regex("^(p\\d+_)?favorite_items$").matches(key) ||
            Regex("^(p\\d+_)?recents(?:_[0-9a-f]+)?$").matches(key)

    private fun isSecureSetKey(key: String): Boolean =
        Regex("^(p\\d+_)?favorites$").matches(key)

    private fun secureString(key: String): String {
        secure.getString(key)?.let { return it }
        val legacy = prefs.getString(key, null) ?: return ""
        secure.putString(key, legacy)
        prefs.edit().remove(key).apply()
        return legacy
    }

    private fun putPreferenceValue(editor: android.content.SharedPreferences.Editor, key: String, value: Any?) {
        when (value) {
            is String -> editor.putString(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
        }
    }
}
