package com.prelude.iptv.ui.sources

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ImportedM3uFile(val path: String, val label: String)

suspend fun importM3uFile(context: Context, uri: Uri): Result<ImportedM3uFile> =
    withContext(Dispatchers.IO) {
        try {
            val directory = File(context.filesDir, "playlists").apply { mkdirs() }
            val target = File(directory, "pl_${System.currentTimeMillis()}.m3u")
            val input = context.contentResolver.openInputStream(uri)
                ?: error("Δεν άνοιξε το επιλεγμένο αρχείο.")
            input.use { source -> target.outputStream().use { destination -> source.copyTo(destination) } }
            val header = target.inputStream().use { stream ->
                val bytes = ByteArray(4096)
                val count = stream.read(bytes).coerceAtLeast(0)
                String(bytes, 0, count)
            }
            if (!header.contains("#EXTM3U") && !header.contains("#EXTINF")) {
                target.delete()
                error("Το αρχείο δεν μοιάζει με έγκυρη λίστα M3U.")
            }
            Result.success(
                ImportedM3uFile(
                    path = target.absolutePath,
                    label = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "playlist.m3u" }
                        ?: "playlist.m3u",
                ),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
