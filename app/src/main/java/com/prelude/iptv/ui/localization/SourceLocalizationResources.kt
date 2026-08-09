package com.prelude.iptv.ui.localization

import androidx.annotation.StringRes
import com.prelude.iptv.R
import com.prelude.iptv.ui.sources.M3uImportFailure
import com.prelude.iptv.ui.sources.PlaylistConnectionFailure
import com.prelude.iptv.ui.sources.PlaylistSourceDetectionKind
import com.prelude.iptv.ui.sources.PlaylistSourceSubmissionFailure
import com.prelude.iptv.ui.sources.PlaylistSourceValidationReason
import com.prelude.iptv.ui.components.settings.SettingsSourceStatus

@StringRes
fun PlaylistSourceValidationReason.messageRes(): Int = when (this) {
    PlaylistSourceValidationReason.PLAYLIST_URL_REQUIRED -> R.string.sources_validation_playlist_url_required
    PlaylistSourceValidationReason.PLAYLIST_URL_INVALID -> R.string.sources_validation_playlist_url_invalid
    PlaylistSourceValidationReason.SERVER_REQUIRED -> R.string.sources_validation_server_required
    PlaylistSourceValidationReason.SERVER_INVALID -> R.string.sources_validation_server_invalid
    PlaylistSourceValidationReason.USERNAME_REQUIRED -> R.string.sources_validation_username_required
    PlaylistSourceValidationReason.PASSWORD_REQUIRED -> R.string.sources_validation_password_required
    PlaylistSourceValidationReason.PORTAL_REQUIRED -> R.string.sources_validation_portal_required
    PlaylistSourceValidationReason.PORTAL_INVALID -> R.string.sources_validation_portal_invalid
    PlaylistSourceValidationReason.MAC_INVALID -> R.string.sources_validation_mac_invalid
    PlaylistSourceValidationReason.FILE_REQUIRED -> R.string.sources_validation_file_required
}

@StringRes
fun PlaylistSourceDetectionKind.labelRes(): Int = when (this) {
    PlaylistSourceDetectionKind.PORTAL_MAC -> R.string.sources_method_mac_title
    PlaylistSourceDetectionKind.XTREAM -> R.string.sources_method_xtream_title
    PlaylistSourceDetectionKind.PLAYLIST_URL -> R.string.sources_method_url_summary
}

@StringRes
fun PlaylistConnectionFailure.messageRes(): Int = when (this) {
    PlaylistConnectionFailure.INVALID_M3U -> R.string.sources_failure_invalid_m3u
    PlaylistConnectionFailure.CREDENTIALS_REJECTED -> R.string.sources_failure_credentials
    PlaylistConnectionFailure.SERVER_NOT_FOUND -> R.string.sources_failure_server_not_found
    PlaylistConnectionFailure.TIMEOUT -> R.string.sources_failure_timeout
    PlaylistConnectionFailure.CONNECTION_REFUSED -> R.string.sources_failure_refused
    PlaylistConnectionFailure.NO_INTERNET -> R.string.sources_failure_no_internet
    PlaylistConnectionFailure.UNKNOWN -> R.string.sources_failure_unknown
}

@StringRes
fun PlaylistSourceSubmissionFailure.messageRes(): Int = when (this) {
    is PlaylistSourceSubmissionFailure.Connection -> reason.messageRes()
    PlaylistSourceSubmissionFailure.Preparation -> R.string.sources_failure_preparation
}

@StringRes
fun M3uImportFailure.messageRes(): Int = when (this) {
    M3uImportFailure.OPEN_FAILED -> R.string.sources_file_open_failed
    M3uImportFailure.INVALID_FILE -> R.string.sources_failure_invalid_m3u
}

@StringRes
fun SettingsSourceStatus.labelRes(): Int = when (this) {
    SettingsSourceStatus.Loading -> R.string.sources_status_loading
    SettingsSourceStatus.Active -> R.string.sources_status_active
    SettingsSourceStatus.Selected -> R.string.sources_status_selected
    SettingsSourceStatus.Saved -> R.string.sources_status_saved
}

@StringRes
fun refreshDaysLabelRes(days: Int): Int = when (days) {
    1 -> R.string.sources_refresh_daily
    7 -> R.string.sources_refresh_weekly
    else -> R.string.sources_refresh_three_days
}
