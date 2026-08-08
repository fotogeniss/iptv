package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityCryptoTest {
    @Test
    fun pinHashIsSaltedAndVerifies() {
        val first = PinHasher.hash("1234")
        val second = PinHasher.hash("1234")
        assertNotEquals(first, second)
        assertTrue(PinHasher.verify("1234", first))
        assertFalse(PinHasher.verify("4321", first))
    }

    @Test
    fun portableBackupRoundTripsAndRejectsWrongPassword() {
        val original = "credentials-and-history"
        val encrypted = PortableBackupCrypto.encrypt(original, "strong-pass")
        assertEquals(original, PortableBackupCrypto.decrypt(encrypted, "strong-pass"))
        assertThrows(IllegalArgumentException::class.java) {
            PortableBackupCrypto.decrypt(encrypted, "wrong-pass")
        }
    }
}
