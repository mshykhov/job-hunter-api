package com.mshykhov.jobhunter.infrastructure.ai

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
@Converter
class AiEncryptionConverter(
    private val aiProperties: AiProperties,
) : AttributeConverter<String, String> {
    private val secretKey: SecretKeySpec by lazy {
        val keyBytes = Base64.getDecoder().decode(aiProperties.encryptionKey)
        require(keyBytes.size == KEY_LENGTH_BYTES) {
            "AI_ENCRYPTION_KEY must decode to exactly $KEY_LENGTH_BYTES bytes"
        }
        SecretKeySpec(keyBytes, "AES")
    }

    override fun convertToDatabaseColumn(attribute: String?): String? {
        if (attribute == null) return null
        val iv = ByteArray(IV_LENGTH_BYTES).also { SECURE_RANDOM.nextBytes(it) }
        val cipher =
            Cipher.getInstance(ALGORITHM).apply {
                init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            }
        val ciphertext = cipher.doFinal(attribute.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv + ciphertext)
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        if (dbData == null) return null
        return try {
            val combined = Base64.getDecoder().decode(dbData)
            require(combined.size > IV_LENGTH_BYTES) { "Encrypted data is too short" }
            val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
            val ciphertext = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)
            val cipher =
                Cipher.getInstance(ALGORITHM).apply {
                    init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
                }
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to decrypt API key — possible key rotation or data corruption", e)
        }
    }

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val IV_LENGTH_BYTES = 12
        private const val TAG_LENGTH_BITS = 128
        private const val KEY_LENGTH_BYTES = 32
        private val SECURE_RANDOM = SecureRandom()
    }
}
