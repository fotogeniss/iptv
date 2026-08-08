package com.prelude.iptv.player

/**
 * Pure, Android-free decisions for external (SRT) subtitle presentation.
 *
 * Extracted from `PlayerActivity` so cue timing, size clamping and the
 * Windows-1253 decode fallback are unit-testable without an Activity, a running
 * ExoPlayer/VLC engine or a `View`. `PlayerActivity` keeps every View/engine
 * side effect and only asks this policy for values, which preserves the legacy
 * behaviour exactly.
 */
object PlayerSubtitlePolicy {
    const val MIN_SIZE_PERCENT = 70
    const val MAX_SIZE_PERCENT = 180
    const val SIZE_STEP = 10
    const val MOBILE_BASE_SP = 18f
    const val TV_BASE_SP = 32f
    const val EXO_BASE_FRACTION = 0.0533f
    const val PREVIEW_MS = 1800L

    /** Clamp a requested size percent into the supported range. */
    fun coerceSizePercent(percent: Int): Int =
        percent.coerceIn(MIN_SIZE_PERCENT, MAX_SIZE_PERCENT)

    fun canGrow(percent: Int): Boolean = percent < MAX_SIZE_PERCENT

    fun canShrink(percent: Int): Boolean = percent > MIN_SIZE_PERCENT

    /** 1.0f at 100%. */
    fun scale(percent: Int): Float = percent / 100f

    /** Base text size for our own TextView-rendered SRT cues. */
    fun baseSp(isTv: Boolean): Float = if (isTv) TV_BASE_SP else MOBILE_BASE_SP

    /** Fractional size handed to the ExoPlayer subtitle view for embedded cues. */
    fun exoFraction(percent: Int): Float = EXO_BASE_FRACTION * scale(percent)

    /**
     * The cue that should be visible at [positionMs], or `null` when none apply.
     * First match wins, matching the previous inline `firstOrNull` behaviour.
     */
    fun activeCue(cues: List<Cue>, positionMs: Long): Cue? =
        cues.firstOrNull { positionMs in it.startMs..it.endMs }

    /**
     * Many Greek OpenSubtitles SRT files are Windows-1253, not UTF-8. Decoding
     * as UTF-8 first and only falling back when the replacement char (U+FFFD)
     * appears keeps valid UTF-8 intact while rescuing legacy encodings.
     */
    fun decodeSubtitleBytes(bytes: ByteArray): String {
        val payload = unpackSubtitleBytes(bytes)
        val utf = String(payload, Charsets.UTF_8)
        if (!utf.contains('\uFFFD')) return utf
        return runCatching { String(payload, charset("windows-1253")) }.getOrDefault(utf)
    }

    /** OpenSubtitles download links may return either a plain SRT or gzip bytes. */
    internal fun unpackSubtitleBytes(bytes: ByteArray): ByteArray {
        val gzip = bytes.size >= 2 &&
            (bytes[0].toInt() and 0xff) == 0x1f &&
            (bytes[1].toInt() and 0xff) == 0x8b
        if (!gzip) return bytes
        return runCatching {
            java.util.zip.GZIPInputStream(bytes.inputStream()).use { it.readBytes() }
        }.getOrDefault(bytes)
    }
}
