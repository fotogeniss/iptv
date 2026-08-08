package com.prelude.iptv.tvhome

import android.content.Context
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.SecureStorage
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class TvHomeEntry(
    val token: String,
    val profileId: Int,
    val sourceId: String,
    val itemKey: String,
    val channel: Channel,
    val positionMs: Long,
    val durationMs: Long,
    val lastEngagementMs: Long
) {
    val identity: String get() = "$profileId|$sourceId|$itemKey"
}

/** Stores launcher playback payloads encrypted; the TV Provider only sees opaque tokens. */
internal class TvHomeEntryStore(context: Context) {
    private val secure = SecureStorage(context.applicationContext)

    fun replace(candidates: List<TvHomeCandidate>): List<TvHomeEntry> {
        val existingTokens = load().associate { it.identity to it.token }
        val suppressions = loadSuppressions().toMutableMap()
        val entries = candidates.mapNotNull { candidate ->
            val suppressedAt = suppressions[candidate.identity]
            if (!TvHomeSuppressionPolicy.shouldPublish(candidate.lastEngagementMs, suppressedAt)) return@mapNotNull null
            if (suppressedAt != null) suppressions.remove(candidate.identity)
            TvHomeEntry(
                token = existingTokens[candidate.identity] ?: UUID.randomUUID().toString(),
                profileId = candidate.profileId,
                sourceId = candidate.sourceId,
                itemKey = candidate.itemKey,
                channel = candidate.channel,
                positionMs = candidate.positionMs,
                durationMs = candidate.durationMs,
                lastEngagementMs = candidate.lastEngagementMs
            )
        }
        secure.putString(KEY, JSONArray().apply { entries.forEach { put(it.toJson()) } }.toString())
        saveSuppressions(suppressions)
        return entries
    }

    fun resolve(token: String): TvHomeEntry? =
        token.takeIf(String::isNotBlank)?.let { wanted -> load().firstOrNull { it.token == wanted } }

    fun suppress(entry: TvHomeEntry) {
        val suppressions = loadSuppressions().toMutableMap()
        suppressions[entry.identity] = entry.lastEngagementMs
        saveSuppressions(suppressions)
    }

    fun clear() {
        secure.remove(KEY)
        secure.remove(SUPPRESSIONS_KEY)
    }

    private fun load(): List<TvHomeEntry> {
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
                val engagement = obj.optLong("engagement", 0L)
                if (identity.isNotBlank() && engagement > 0L) put(identity, engagement)
            }
        }
    }

    private fun saveSuppressions(values: Map<String, Long>) {
        val array = JSONArray()
        values.forEach { (identity, engagement) ->
            array.put(JSONObject().put("identity", identity).put("engagement", engagement))
        }
        secure.putString(SUPPRESSIONS_KEY, array.toString())
    }

    private fun TvHomeEntry.toJson(): JSONObject = JSONObject()
        .put("token", token)
        .put("profileId", profileId)
        .put("sourceId", sourceId)
        .put("itemKey", itemKey)
        .put("positionMs", positionMs)
        .put("durationMs", durationMs)
        .put("lastEngagementMs", lastEngagementMs)
        .put("channel", channel.toJson())

    private fun fromJson(obj: JSONObject): TvHomeEntry = TvHomeEntry(
        token = obj.getString("token"),
        profileId = obj.getInt("profileId"),
        sourceId = obj.getString("sourceId"),
        itemKey = obj.getString("itemKey"),
        positionMs = obj.getLong("positionMs"),
        durationMs = obj.getLong("durationMs"),
        lastEngagementMs = obj.optLong("lastEngagementMs", 0L),
        channel = obj.getJSONObject("channel").toChannel()
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
        const val KEY = "tv_home_entries_v1"
        const val SUPPRESSIONS_KEY = "tv_home_suppressions_v1"
    }
}
