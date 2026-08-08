package com.prelude.iptv.ui

/** Pure selection rules for the TV two-step Multiview flow. */
object MultiviewSelectionPolicy {
    sealed interface OpenDecision {
        data object PlaySingle : OpenDecision
        data object KeepPrimaryArmed : OpenDecision
        data class Launch(val primaryKey: String, val secondaryKey: String) : OpenDecision
    }

    fun onOpen(armedPrimaryKey: String?, openedKey: String): OpenDecision {
        if (armedPrimaryKey.isNullOrBlank()) return OpenDecision.PlaySingle
        if (armedPrimaryKey == openedKey) return OpenDecision.KeepPrimaryArmed
        return OpenDecision.Launch(armedPrimaryKey, openedKey)
    }
}
