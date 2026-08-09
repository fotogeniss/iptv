package com.prelude.iptv.ui.localization

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.prelude.iptv.R
import com.prelude.iptv.data.BackupFailure
import com.prelude.iptv.data.toBackupFailure
import com.prelude.iptv.ui.profile.ProfileDisplayName

@Composable
fun localizedProfileName(name: ProfileDisplayName): String = when (name) {
    ProfileDisplayName.Primary -> stringResource(R.string.account_primary_profile_name)
    is ProfileDisplayName.Stored -> name.value
}

@StringRes
fun BackupFailure.messageRes(): Int = when (this) {
    BackupFailure.PasswordTooShort -> R.string.account_backup_error_password_short
    BackupFailure.NotPreludeBackup -> R.string.account_backup_error_wrong_app
    BackupFailure.NewerAppVersion -> R.string.account_backup_error_newer_version
    BackupFailure.MissingData -> R.string.account_backup_error_missing_data
    BackupFailure.InvalidJson -> R.string.account_backup_error_invalid_json
    BackupFailure.InvalidCryptoParameters -> R.string.account_backup_error_crypto_parameters
    BackupFailure.WrongPassword -> R.string.account_backup_error_wrong_password
    BackupFailure.CorruptOrWrongPassword -> R.string.account_backup_error_corrupt_or_password
    BackupFailure.DestinationUnavailable -> R.string.account_backup_error_destination
    BackupFailure.SourceUnavailable -> R.string.account_backup_error_source
    BackupFailure.WriteFailed -> R.string.account_backup_error_write
    BackupFailure.Unknown -> R.string.account_backup_error_unknown
}

fun Context.localizedBackupFailure(error: Throwable): String =
    getString(error.toBackupFailure().messageRes())

fun Context.localizedBackupRestoreSuccess(count: Int): String =
    resources.getQuantityString(R.plurals.account_backup_restored_restart, count, count)
