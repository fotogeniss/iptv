package com.prelude.iptv.data

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Slow, salted verification for the 4-6 digit parental PIN. */
internal object PinHasher {
    private const val PREFIX = "pbkdf2-sha256"
    private const val ITERATIONS = 150_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    fun hash(pin: String): String {
        require(pin.length in 4..6 && pin.all(Char::isDigit)) { "Το PIN πρέπει να έχει 4-6 ψηφία" }
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val derived = derive(pin, salt, ITERATIONS)
        return listOf(
            PREFIX,
            ITERATIONS.toString(),
            Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(derived)
        ).joinToString("$")
    }

    fun verify(pin: String, encoded: String): Boolean = runCatching {
        val parts = encoded.split('$')
        if (parts.size != 4 || parts[0] != PREFIX) return false
        val iterations = parts[1].toInt()
        if (iterations !in 50_000..1_000_000) return false
        val salt = Base64.getDecoder().decode(parts[2])
        val expected = Base64.getDecoder().decode(parts[3])
        MessageDigest.isEqual(expected, derive(pin, salt, iterations))
    }.getOrDefault(false)

    fun isHash(value: String): Boolean = value.startsWith("$PREFIX$")

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val chars = pin.toCharArray()
        return try {
            val spec = PBEKeySpec(chars, salt, iterations, KEY_BITS)
            try {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            } finally {
                spec.clearPassword()
            }
        } finally {
            chars.fill('\u0000')
        }
    }
}
