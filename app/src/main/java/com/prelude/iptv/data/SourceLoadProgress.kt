package com.prelude.iptv.data

/**
 * Transient, source-scoped loading state. Catalog contents are never persisted;
 * this model only describes the currently running network/parse operation.
 */
data class SourceLoadProgress(
    val percent: Int? = null,
    val stage: String = "",
    val contentType: String = "live",
    val active: Boolean = false
)

typealias SourceProgressCallback = (percent: Int?, stage: String) -> Unit

/** Publishes an immutable accumulated catalog while provider loading continues. */
typealias SourcePartialCallback = (items: List<Channel>) -> Unit
