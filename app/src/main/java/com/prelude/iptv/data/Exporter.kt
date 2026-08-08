package com.prelude.iptv.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object Exporter {

    /** M3U με άμεσα URLs (για M3U/Xtream — όχι για MAC που θέλει relay). */
    fun buildDirectM3u(channels: List<Channel>): String {
        val sb = StringBuilder("#EXTM3U\n")
        channels.forEach { ch ->
            if (ch.url.isNotEmpty()) {
                sb.append(
                    "#EXTINF:-1 tvg-id=\"${ch.tvgId}\" tvg-logo=\"${ch.logo}\" " +
                        "group-title=\"${ch.group}\",${ch.name}\n"
                )
                sb.append(ch.url + "\n")
            }
        }
        return sb.toString()
    }

    /** Αποθηκεύει στο φάκελο Downloads. Επιστρέφει διαδρομή ή null. */
    fun saveToDownloads(context: Context, filename: String, content: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, filename)
                    put(MediaStore.Downloads.MIME_TYPE, "audio/x-mpegurl")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                "Downloads/$filename"
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val f = File(dir, filename)
                f.writeText(content)
                f.absolutePath
            }
        } catch (e: Exception) {
            null
        }
    }
}
