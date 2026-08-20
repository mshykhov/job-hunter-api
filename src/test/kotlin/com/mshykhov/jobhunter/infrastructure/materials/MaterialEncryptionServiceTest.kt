package com.mshykhov.jobhunter.infrastructure.materials

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

class MaterialEncryptionServiceTest {
    private val ownerId = UUID.randomUUID()
    private val recordId = UUID.randomUUID()
    private val plaintext = "private application material".toByteArray()

    @Test
    fun `round trips binary content with authenticated context`() {
        val service = serviceWith(randomKey())

        val encrypted = service.encrypt(ownerId, recordId, "CV_PDF", plaintext)

        assertArrayEquals(plaintext, service.decrypt(ownerId, recordId, "CV_PDF", encrypted))
    }

    @Test
    fun `uses a fresh nonce for identical plaintext`() {
        val service = serviceWith(randomKey())

        val first = service.encrypt(ownerId, recordId, "CV_PDF", plaintext)
        val second = service.encrypt(ownerId, recordId, "CV_PDF", plaintext)

        assertFalse(first.contentEquals(second))
    }

    @Test
    fun `rejects tampered ciphertext`() {
        val service = serviceWith(randomKey())
        val encrypted = service.encrypt(ownerId, recordId, "CV_PDF", plaintext)
        encrypted[encrypted.lastIndex] = (encrypted.last() + 1).toByte()

        assertThrows(MaterialDecryptionException::class.java) {
            service.decrypt(ownerId, recordId, "CV_PDF", encrypted)
        }
    }

    @Test
    fun `rejects wrong key or authenticated context`() {
        val service = serviceWithKey(KEY)
        val encrypted = service.encrypt(ownerId, recordId, "CV_PDF", plaintext)

        assertThrows(MaterialDecryptionException::class.java) {
            serviceWith(randomKey()).decrypt(ownerId, recordId, "CV_PDF", encrypted)
        }
        assertThrows(MaterialDecryptionException::class.java) {
            service.decrypt(ownerId, recordId, "CV_DOCX", encrypted)
        }
    }

    @Test
    fun `requires a base64 encoded 256 bit key`() {
        assertThrows(IllegalArgumentException::class.java) {
            serviceWith("not-base64")
        }
        assertThrows(IllegalArgumentException::class.java) {
            serviceWith(Base64.getEncoder().encodeToString(ByteArray(16)))
        }
    }

    private fun serviceWith(key: String) = MaterialEncryptionService(MaterialEncryptionProperties(key))

    private fun serviceWithKey(key: ByteArray) = serviceWith(Base64.getEncoder().encodeToString(key))

    private fun randomKey(): String = Base64.getEncoder().encodeToString(ByteArray(32).also(SecureRandom()::nextBytes))

    private companion object {
        val KEY = ByteArray(32) { it.toByte() }
    }
}
