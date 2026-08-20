package com.mshykhov.jobhunter.infrastructure.materials

import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class MaterialEncryptionService(properties: MaterialEncryptionProperties) {
    private val secretKey =
        try {
            Base64.getDecoder().decode(properties.encryptionKey)
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException("MATERIAL_ENCRYPTION_KEY must be valid base64", exception)
        }.also {
            require(it.size == KEY_LENGTH_BYTES) {
                "MATERIAL_ENCRYPTION_KEY must decode to exactly $KEY_LENGTH_BYTES bytes"
            }
        }.let { SecretKeySpec(it, "AES") }

    fun encrypt(
        ownerId: UUID,
        recordId: UUID,
        kind: String,
        plaintext: ByteArray,
    ): ByteArray {
        val nonce = ByteArray(NONCE_LENGTH_BYTES).also(SECURE_RANDOM::nextBytes)
        val cipher = initializedCipher(Cipher.ENCRYPT_MODE, nonce, ownerId, recordId, kind)
        return byteArrayOf(ENVELOPE_VERSION) + nonce + cipher.doFinal(plaintext)
    }

    fun decrypt(
        ownerId: UUID,
        recordId: UUID,
        kind: String,
        envelope: ByteArray,
    ): ByteArray {
        try {
            require(envelope.size > HEADER_LENGTH_BYTES + TAG_LENGTH_BYTES)
            require(envelope[0] == ENVELOPE_VERSION)
            val nonce = envelope.copyOfRange(1, HEADER_LENGTH_BYTES)
            val ciphertext = envelope.copyOfRange(HEADER_LENGTH_BYTES, envelope.size)
            return initializedCipher(Cipher.DECRYPT_MODE, nonce, ownerId, recordId, kind).doFinal(ciphertext)
        } catch (exception: Exception) {
            throw MaterialDecryptionException(exception)
        }
    }

    private fun initializedCipher(
        mode: Int,
        nonce: ByteArray,
        ownerId: UUID,
        recordId: UUID,
        kind: String,
    ): Cipher =
        Cipher.getInstance(ALGORITHM).apply {
            init(mode, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, nonce))
            updateAAD(authenticatedContext(ownerId, recordId, kind))
        }

    private fun authenticatedContext(
        ownerId: UUID,
        recordId: UUID,
        kind: String,
    ): ByteArray {
        val kindBytes = kind.toByteArray(Charsets.UTF_8)
        return ByteBuffer
            .allocate(UUID_BYTES * 2 + Int.SIZE_BYTES + kindBytes.size)
            .putLong(ownerId.mostSignificantBits)
            .putLong(ownerId.leastSignificantBits)
            .putLong(recordId.mostSignificantBits)
            .putLong(recordId.leastSignificantBits)
            .putInt(kindBytes.size)
            .put(kindBytes)
            .array()
    }

    private companion object {
        const val ALGORITHM = "AES/GCM/NoPadding"
        const val KEY_LENGTH_BYTES = 32
        const val NONCE_LENGTH_BYTES = 12
        const val TAG_LENGTH_BYTES = 16
        const val TAG_LENGTH_BITS = TAG_LENGTH_BYTES * Byte.SIZE_BITS
        const val HEADER_LENGTH_BYTES = 1 + NONCE_LENGTH_BYTES
        const val UUID_BYTES = 16
        const val ENVELOPE_VERSION: Byte = 1
        val SECURE_RANDOM = SecureRandom()
    }
}
