package com.prelude.iptv.data

enum class BackupFailure {
    PasswordTooShort,
    NotPreludeBackup,
    NewerAppVersion,
    MissingData,
    InvalidJson,
    InvalidCryptoParameters,
    WrongPassword,
    CorruptOrWrongPassword,
    DestinationUnavailable,
    SourceUnavailable,
    WriteFailed,
    Unknown,
}

class BackupException(
    val failure: BackupFailure,
    cause: Throwable? = null,
) : IllegalArgumentException(failure.name, cause)

fun Throwable.toBackupFailure(): BackupFailure =
    (this as? BackupException)?.failure ?: BackupFailure.Unknown
