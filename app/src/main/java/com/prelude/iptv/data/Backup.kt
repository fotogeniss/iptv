package com.prelude.iptv.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Password-protected, device-independent backup.
 *
 * The complete payload (including provider credentials, favorites/history and
 * PIN hash) is encrypted with AES-256-GCM. The password is never stored.
 */
object Backup {
    private const val VERSION = 2
    private const val MAGIC = "UltimateIPTV-Backup"

    /** Keys that are specific to the current runtime rather than user data. */
    private val SKIP = setOf("last_playlist")

    fun export(ctx: Context, password: String): String {
        PortableBackupCrypto.requirePassword(password)
        // Constructing the store performs all plaintext-to-Keystore migrations.
        val store = PlaylistStore(ctx)
        val playlists = store.loadPlaylists()
        playlists.forEach(store::migrateLegacyPlaylistKeys)
        store.purgeUnsafeLegacyKeys()

        val prefs = ctx.getSharedPreferences(PlaylistStore.PREFS, Context.MODE_PRIVATE)
        val entries = JSONArray()
        for ((key, value) in prefs.all) {
            if (key in SKIP || value == null) continue
            encodePreference(key, value)?.let(entries::put)
        }

        val secureEntries = JSONArray()
        SecureStorage(ctx).snapshot().toSortedMap().forEach { (key, value) ->
            secureEntries.put(JSONObject().put("k", key).put("v", value))
        }

        val payload = JSONObject()
            .put("createdAt", System.currentTimeMillis())
            .put("entries", entries)
            .put("secureEntries", secureEntries)
            .toString()

        return encryptEnvelope(payload, password)
    }

    /**
     * Restores the encrypted v2 format and also accepts old plaintext v1 files.
     * Old files are immediately migrated into Keystore-backed persistence.
     */
    fun import(ctx: Context, json: String, password: String): Int {
        val root = parseRoot(json)
        if (root.optString("app") != MAGIC) {
            throw IllegalArgumentException("Δεν είναι αρχείο αντιγράφου αυτής της εφαρμογής")
        }

        val version = root.optInt("version", 1)
        if (version > VERSION) {
            throw IllegalArgumentException("Το αρχείο είναι από νεότερη έκδοση της εφαρμογής")
        }

        val payload = if (root.optBoolean("encrypted", false)) {
            PortableBackupCrypto.requirePassword(password)
            JSONObject(decryptEnvelope(root, password))
        } else {
            // Legacy v1: plaintext root contained entries directly.
            root
        }

        val entries = payload.optJSONArray("entries")
            ?: throw IllegalArgumentException("Το αρχείο δεν περιέχει δεδομένα")
        val secureEntries = payload.optJSONArray("secureEntries") ?: JSONArray()

        val prefs = ctx.getSharedPreferences(PlaylistStore.PREFS, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val secure = SecureStorage(ctx)
        secure.clear()

        var restored = restorePreferences(prefs, entries)
        for (i in 0 until secureEntries.length()) {
            val item = secureEntries.optJSONObject(i) ?: continue
            val key = item.optString("k")
            if (key.isBlank()) continue
            secure.putString(key, item.optString("v"))
            restored++
        }

        // Migrates sensitive values from legacy v1 entries and removes raw IDs.
        val store = PlaylistStore(ctx)
        val playlists = store.loadPlaylists()
        playlists.forEach(store::migrateLegacyPlaylistKeys)
        store.purgeUnsafeLegacyKeys()
        return restored
    }

    fun suggestedFileName(): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "iptv-backup-$date.json"
    }

    private fun encryptEnvelope(payload: String, password: String): String {
        val encrypted = PortableBackupCrypto.encrypt(payload, password)
        return JSONObject()
            .put("app", MAGIC)
            .put("version", VERSION)
            .put("encrypted", true)
            .put("kdf", "PBKDF2WithHmacSHA256")
            .put("iterations", encrypted.iterations)
            .put("salt", encrypted.salt)
            .put("iv", encrypted.iv)
            .put("ciphertext", encrypted.ciphertext)
            .toString(2)
    }

    private fun decryptEnvelope(root: JSONObject, password: String): String =
        PortableBackupCrypto.decrypt(
            EncryptedBackupPayload(
                iterations = root.optInt("iterations", 0),
                salt = root.optString("salt"),
                iv = root.optString("iv"),
                ciphertext = root.optString("ciphertext")
            ),
            password
        )

    private fun restorePreferences(prefs: SharedPreferences, entries: JSONArray): Int {
        val editor = prefs.edit()
        var count = 0
        for (i in 0 until entries.length()) {
            val item = entries.optJSONObject(i) ?: continue
            val key = item.optString("k")
            if (key.isBlank() || key in SKIP) continue
            when (item.optString("t")) {
                "s" -> editor.putString(key, item.optString("v"))
                "i" -> editor.putInt(key, item.optInt("v"))
                "l" -> editor.putLong(key, item.optLong("v"))
                "f" -> editor.putFloat(key, item.optDouble("v").toFloat())
                "b" -> editor.putBoolean(key, item.optBoolean("v"))
                "ss" -> {
                    val values = item.optJSONArray("v") ?: continue
                    editor.putStringSet(key, (0 until values.length()).map { values.getString(it) }.toSet())
                }
                else -> continue
            }
            count++
        }
        if (!editor.commit()) throw IllegalStateException("Αποτυχία εγγραφής αντιγράφου")
        return count
    }

    private fun encodePreference(key: String, value: Any): JSONObject? {
        val item = JSONObject().put("k", key)
        return when (value) {
            is String -> item.put("t", "s").put("v", value)
            is Int -> item.put("t", "i").put("v", value)
            is Long -> item.put("t", "l").put("v", value)
            is Float -> item.put("t", "f").put("v", value.toDouble())
            is Boolean -> item.put("t", "b").put("v", value)
            is Set<*> -> item.put("t", "ss").put("v", JSONArray(value.map { it.toString() }))
            else -> null
        }
    }

    private fun parseRoot(json: String): JSONObject =
        runCatching { JSONObject(json) }.getOrElse {
            throw IllegalArgumentException("Δεν είναι έγκυρο αρχείο JSON")
        }

}
