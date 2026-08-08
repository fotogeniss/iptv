package com.prelude.iptv.data

/** Chooses the automatic XMLTV source without performing network or storage work. */
object EpgAutoSourcePolicy {
    fun choose(customUrl: String, embeddedM3uUrl: String, xtreamUrl: String): String =
        sequenceOf(customUrl, embeddedM3uUrl, xtreamUrl)
            .map(String::trim)
            .firstOrNull(String::isNotBlank)
            .orEmpty()
}
