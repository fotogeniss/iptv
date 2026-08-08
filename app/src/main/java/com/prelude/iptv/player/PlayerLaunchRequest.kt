package com.prelude.iptv.player

import android.content.Context
import android.content.Intent
import com.prelude.iptv.PlayerActivity
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.SubtitleSearchPolicy
import com.prelude.iptv.data.SubtitleSearchRequest

/**
 * Single, typed contract for every route that opens [PlayerActivity].
 *
 * Keeping all extra names and fallbacks here prevents reminder, TV-home,
 * direct-stream and catalog routes from silently drifting apart.
 */
data class PlayerLaunchRequest(
    val url: String,
    val title: String,
    val kind: String = "live",
    val tvgId: String = "",
    val logo: String = "",
    val sourceId: String = "",
    val positionKey: String = "",
    val plot: String = "",
    val cast: String = "",
    val director: String = "",
    val genre: String = "",
    val year: String = "",
    val duration: String = "",
    val subtitle: SubtitleSearchRequest = SubtitleSearchRequest(title = title),
    val epgTitles: List<String> = emptyList(),
    val epgTimes: List<String> = emptyList(),
    val epgDescriptions: List<String> = emptyList(),
) {
    init {
        require(url.isNotBlank()) { "Player URL must not be blank" }
    }

    fun toIntent(context: Context): Intent = Intent(context, PlayerActivity::class.java).apply {
        putExtra(EXTRA_URL, url)
        putExtra(EXTRA_TITLE, title)
        putExtra(EXTRA_KIND, kind)
        putExtra(EXTRA_TVG_ID, tvgId)
        putExtra(EXTRA_LOGO, logo)
        putExtra(EXTRA_SOURCE_ID, sourceId)
        putExtra(EXTRA_POSITION_KEY, positionKey)
        putExtra(EXTRA_PLOT, plot)
        putExtra(EXTRA_CAST, cast)
        putExtra(EXTRA_DIRECTOR, director)
        putExtra(EXTRA_GENRE, genre)
        putExtra(EXTRA_YEAR, year)
        putExtra(EXTRA_DURATION, duration)
        putExtra(EXTRA_SUBTITLE_TITLE, subtitle.title)
        putExtra(EXTRA_SUBTITLE_YEAR, subtitle.year ?: 0)
        putExtra(EXTRA_SUBTITLE_SEASON, subtitle.season ?: 0)
        putExtra(EXTRA_SUBTITLE_EPISODE, subtitle.episode ?: 0)
        putExtra(EXTRA_SUBTITLE_TYPE, subtitle.type)
        if (epgTitles.isNotEmpty()) putStringArrayListExtra(EXTRA_EPG_TITLES, ArrayList(epgTitles))
        if (epgTimes.isNotEmpty()) putStringArrayListExtra(EXTRA_EPG_TIMES, ArrayList(epgTimes))
        if (epgDescriptions.isNotEmpty()) {
            putStringArrayListExtra(EXTRA_EPG_DESCRIPTIONS, ArrayList(epgDescriptions))
        }
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_KIND = "kind"
        const val EXTRA_TVG_ID = "tvgId"
        const val EXTRA_LOGO = "logo"
        const val EXTRA_SOURCE_ID = "sourceId"
        const val EXTRA_POSITION_KEY = "posKey"
        const val EXTRA_PLOT = "plot"
        const val EXTRA_CAST = "cast"
        const val EXTRA_DIRECTOR = "director"
        const val EXTRA_GENRE = "genre"
        const val EXTRA_YEAR = "year"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_SUBTITLE_TITLE = "subtitleTitle"
        const val EXTRA_SUBTITLE_YEAR = "subtitleYear"
        const val EXTRA_SUBTITLE_SEASON = "subtitleSeason"
        const val EXTRA_SUBTITLE_EPISODE = "subtitleEpisode"
        const val EXTRA_SUBTITLE_TYPE = "subtitleType"
        const val EXTRA_EPG_TITLES = "epgTitles"
        const val EXTRA_EPG_TIMES = "epgTimes"
        const val EXTRA_EPG_DESCRIPTIONS = "epgDescs"

        fun fromIntent(intent: Intent, fallbackSourceId: String = ""): PlayerLaunchRequest? {
            val url = intent.getStringExtra(EXTRA_URL)?.trim().orEmpty()
            if (url.isBlank()) return null
            val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
            val kind = intent.getStringExtra(EXTRA_KIND).orEmpty().ifBlank { "live" }
            val year = intent.getStringExtra(EXTRA_YEAR).orEmpty()
            val fallbackSubtitle = SubtitleSearchPolicy.fromChannel(
                Channel(name = title, kind = kind, year = year)
            )
            val subtitle = SubtitleSearchRequest(
                title = intent.getStringExtra(EXTRA_SUBTITLE_TITLE).orEmpty()
                    .ifBlank { fallbackSubtitle.title },
                year = intent.getIntExtra(EXTRA_SUBTITLE_YEAR, 0).takeIf { it > 0 }
                    ?: fallbackSubtitle.year,
                season = intent.getIntExtra(EXTRA_SUBTITLE_SEASON, 0).takeIf { it > 0 }
                    ?: fallbackSubtitle.season,
                episode = intent.getIntExtra(EXTRA_SUBTITLE_EPISODE, 0).takeIf { it > 0 }
                    ?: fallbackSubtitle.episode,
                type = intent.getStringExtra(EXTRA_SUBTITLE_TYPE).orEmpty()
                    .ifBlank { fallbackSubtitle.type },
            )
            return PlayerLaunchRequest(
                url = url,
                title = title,
                kind = kind,
                tvgId = intent.getStringExtra(EXTRA_TVG_ID).orEmpty(),
                logo = intent.getStringExtra(EXTRA_LOGO).orEmpty(),
                sourceId = intent.getStringExtra(EXTRA_SOURCE_ID).orEmpty().ifBlank { fallbackSourceId },
                positionKey = intent.getStringExtra(EXTRA_POSITION_KEY).orEmpty(),
                plot = intent.getStringExtra(EXTRA_PLOT).orEmpty(),
                cast = intent.getStringExtra(EXTRA_CAST).orEmpty(),
                director = intent.getStringExtra(EXTRA_DIRECTOR).orEmpty(),
                genre = intent.getStringExtra(EXTRA_GENRE).orEmpty(),
                year = year,
                duration = intent.getStringExtra(EXTRA_DURATION).orEmpty(),
                subtitle = subtitle,
                epgTitles = intent.getStringArrayListExtra(EXTRA_EPG_TITLES).orEmpty(),
                epgTimes = intent.getStringArrayListExtra(EXTRA_EPG_TIMES).orEmpty(),
                epgDescriptions = intent.getStringArrayListExtra(EXTRA_EPG_DESCRIPTIONS).orEmpty(),
            )
        }

        fun forChannel(
            url: String,
            channel: Channel,
            sourceId: String,
            positionKey: String,
            subtitle: SubtitleSearchRequest = SubtitleSearchPolicy.fromChannel(channel),
            metadata: Map<String, String> = emptyMap(),
            epgTitles: List<String> = emptyList(),
            epgTimes: List<String> = emptyList(),
            epgDescriptions: List<String> = emptyList(),
        ): PlayerLaunchRequest = PlayerLaunchRequest(
            url = url,
            title = channel.name,
            kind = channel.kind,
            tvgId = channel.tvgId,
            logo = channel.logo,
            sourceId = sourceId,
            positionKey = positionKey,
            plot = metadata[EXTRA_PLOT].orEmpty().ifBlank { channel.plot },
            cast = metadata[EXTRA_CAST].orEmpty().ifBlank { channel.cast },
            director = metadata[EXTRA_DIRECTOR].orEmpty().ifBlank { channel.director },
            genre = metadata[EXTRA_GENRE].orEmpty().ifBlank { channel.genre },
            year = metadata[EXTRA_YEAR].orEmpty().ifBlank { channel.year },
            duration = metadata[EXTRA_DURATION].orEmpty().ifBlank { channel.duration },
            subtitle = subtitle,
            epgTitles = epgTitles,
            epgTimes = epgTimes,
            epgDescriptions = epgDescriptions,
        )
    }
}
