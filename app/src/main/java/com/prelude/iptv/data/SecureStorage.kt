package com.prelude.iptv.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small Android-Keystore backed string store.
 *
 * Only ciphertext and a random IV are written to SharedPreferences. The AES key
 * is generated inside AndroidKeyStore and cannot be exported from the device.
 */
internal class SecureStorage(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getString(name: String): String? {
        val encoded = prefs.getString(name, null) ?: return null
        return runCatching {
            val separator = encoded.indexOf('.')
            require(separator > 0 && separator < encoded.lastIndex)
            val iv = Base64.getDecoder().decode(encoded.substring(0, separator))
            val ciphertext = Base64.getDecoder().decode(encoded.substring(separator + 1))
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
        }.getOrNull()
    }

    fun putString(name: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val encoded = Base64.getEncoder().encodeToString(cipher.iv) + "." +
            Base64.getEncoder().encodeToString(ciphertext)
        prefs.edit().putString(name, encoded).apply()
    }

    fun remove(name: String) {
        prefs.edit().remove(name).apply()
    }

    fun clear() {
        prefs.edit().clear().commit()
    }

    fun keys(): Set<String> = prefs.all.keys

    /** Decrypts a snapshot for an already password-encrypted portable backup. */
    fun snapshot(): Map<String, String> = buildMap {
        keys().forEach { key -> getString(key)?.let { put(key, it) } }
    }

    private fun secretKey(): SecretKey = synchronized(KEY_LOCK) {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: run {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generator.generateKey()
        }
    }

    private companion object {
        const val PREFS = "upl_secure_v1"
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "upl_secure_storage_aes_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        val KEY_LOCK = Any()
    }
}
