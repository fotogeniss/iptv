package com.prelude.iptv.data

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal data class EncryptedBackupPayload(
    val iterations: Int,
    val salt: String,
    val iv: String,
    val ciphertext: String
)

/** Pure JVM/Android crypto used by the portable backup envelope. */
internal object PortableBackupCrypto {
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    fun encrypt(payload: String, password: String): EncryptedBackupPayload {
        requirePassword(password)
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val iv = ByteArray(IV_BYTES).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt, ITERATIONS), GCMParameterSpec(TAG_BITS, iv))
        val ciphertext = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))
        return EncryptedBackupPayload(
            iterations = ITERATIONS,
            salt = Base64.getEncoder().encodeToString(salt),
            iv = Base64.getEncoder().encodeToString(iv),
            ciphertext = Base64.getEncoder().encodeToString(ciphertext)
        )
    }

    fun decrypt(payload: EncryptedBackupPayload, password: String): String {
        requirePassword(password)
        return try {
            if (payload.iterations !in 50_000..1_000_000) {
                throw BackupException(BackupFailure.InvalidCryptoParameters)
            }
            val salt = Base64.getDecoder().decode(payload.salt)
            val iv = Base64.getDecoder().decode(payload.iv)
            val ciphertext = Base64.getDecoder().decode(payload.ciphertext)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                deriveKey(password, salt, payload.iterations),
                GCMParameterSpec(TAG_BITS, iv)
            )
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: AEADBadTagException) {
            throw BackupException(BackupFailure.WrongPassword)
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException(BackupFailure.CorruptOrWrongPassword, e)
        }
    }

    fun requirePassword(password: String) {
        if (password.length < 6) throw BackupException(BackupFailure.PasswordTooShort)
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val chars = password.toCharArray()
        return try {
            val spec = PBEKeySpec(chars, salt, iterations, KEY_BITS)
            try {
                SecretKeySpec(
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,
                    "AES"
                )
            } finally {
                spec.clearPassword()
            }
        } finally {
            chars.fill('\u0000')
        }
    }
}
