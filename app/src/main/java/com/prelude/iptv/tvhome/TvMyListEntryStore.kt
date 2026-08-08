package com.prelude.iptv.tvhome

import android.content.Context
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.SecureStorage
import com.prelude.iptv.data.SourceFavorite
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class TvMyListEntry(
    val token: String,
    val profileId: Int,
    val sourceId: String,
    val itemKey: String,
    val channel: Channel,
    val addedAtMs: Long
) {
    val identity: String get() = "$profileId|$sourceId|$itemKey"
}

/** Encrypted launcher payloads for the app-owned My List preview channel. */
internal class TvMyListEntryStore(context: Context) {
    private val secure = SecureStorage(context.applicationContext)

    fun replace(profileId: Int, favorites: List<SourceFavorite>): List<TvMyListEntry> {
        val existingTokens = load().associate { it.identity to it.token }
        val suppressions = loadSuppressions().toMutableMap()
        val entries = favorites.mapNotNull { favorite ->
            val identity = "$profileId|${favorite.identity}"
            val suppressedAt = suppressions[identity]
            if (!TvHomeSuppressionPolicy.shouldPublish(favorite.addedAtMs, suppressedAt)) return@mapNotNull null
            if (suppressedAt != null) suppressions.remove(identity)
            TvMyListEntry(
                token = existingTokens[identity] ?: UUID.randomUUID().toString(),
                profileId = profileId,
                sourceId = favorite.sourceId,
                itemKey = favorite.itemKey,
                channel = favorite.channel,
                addedAtMs = favorite.addedAtMs
            )
        }
        save(entries)
        saveSuppressions(suppressions)
        return entries
    }

    fun resolve(token: String): TvMyListEntry? =
        token.takeIf(String::isNotBlank)?.let { wanted -> load().firstOrNull { it.token == wanted } }

    fun suppress(entry: TvMyListEntry) {
        val suppressions = loadSuppressions().toMutableMap()
        suppressions[entry.identity] = entry.addedAtMs
        saveSuppressions(suppressions)
    }

    fun clear() {
        secure.remove(KEY)
        secure.remove(SUPPRESSIONS_KEY)
    }

    private fun save(entries: List<TvMyListEntry>) {
        secure.putString(KEY, JSONArray().apply { entries.forEach { put(it.toJson()) } }.toString())
    }

    private fun load(): List<TvMyListEntry> {
        val array = runCatching { JSONArray(secure.getString(KEY) ?: "[]") }.getOrDefault(JSONArray())
        return buildList {
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                runCatching { fromJson(obj) }.getOrNull()?.let(::add)
            }
        }
    }

    private fun loadSuppressions(): Map<String, Long> {
        val array = runCatching { JSONArray(secure.getString(SUPPRESSIONS_KEY) ?: "[]") }.getOrDefault(JSONArray())
        return buildMap {
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val identity = obj.optString("identity")
                val addedAt = obj.optLong("addedAt", 0L)
                if (identity.isNotBlank() && addedAt > 0L) put(identity, addedAt)
            }
        }
    }

    private fun saveSuppressions(values: Map<String, Long>) {
        secure.putString(
            SUPPRESSIONS_KEY,
            JSONArray().apply {
                values.forEach { (identity, addedAt) ->
                    put(JSONObject().put("identity", identity).put("addedAt", addedAt))
                }
            }.toString()
        )
    }

    private fun TvMyListEntry.toJson(): JSONObject = JSONObject()
        .put("token", token).put("profileId", profileId).put("sourceId", sourceId)
        .put("itemKey", itemKey).put("addedAtMs", addedAtMs).put("channel", channel.toJson())

    private fun fromJson(obj: JSONObject): TvMyListEntry = TvMyListEntry(
        token = obj.getString("token"), profileId = obj.getInt("profileId"),
        sourceId = obj.getString("sourceId"), itemKey = obj.getString("itemKey"),
        channel = obj.getJSONObject("channel").toChannel(), addedAtMs = obj.optLong("addedAtMs", 0L)
    )

    private fun Channel.toJson(): JSONObject = JSONObject()
        .put("name", name).put("group", group).put("logo", logo).put("tvgId", tvgId)
        .put("url", url).put("cmd", cmd).put("chId", chId).put("streamId", streamId)
        .put("kind", kind).put("seriesId", seriesId).put("plot", plot).put("cast", cast)
        .put("director", director).put("genre", genre).put("year", year).put("duration", duration)

    private fun JSONObject.toChannel(): Channel = Channel(
        name = optString("name"), group = optString("group"), logo = optString("logo"),
        tvgId = optString("tvgId"), url = optString("url"), cmd = optString("cmd"),
        chId = optString("chId"), streamId = optString("streamId"), kind = optString("kind", "vod"),
        seriesId = optString("seriesId"), plot = optString("plot"), cast = optString("cast"),
        director = optString("director"), genre = optString("genre"), year = optString("year"),
        duration = optString("duration")
    )

    private companion object {
        const val KEY = "tv_home_my_list_entries_v1"
        const val SUPPRESSIONS_KEY = "tv_home_my_list_suppressions_v1"
    }
}
