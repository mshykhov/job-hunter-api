package com.mshykhov.jobhunter.infrastructure.materials

import com.mshykhov.jobhunter.application.common.NotFoundException
import com.mshykhov.jobhunter.application.common.ValidationException
import com.mshykhov.jobhunter.application.materials.ApplicationMaterialArtifactEntity
import com.mshykhov.jobhunter.application.materials.ApplicationMaterialArtifactRepository
import com.mshykhov.jobhunter.application.materials.MaterialKind
import com.mshykhov.jobhunter.application.user.UserRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID

@Component
@Transactional(readOnly = true)
class EncryptedMaterialStore(
    private val artifactRepository: ApplicationMaterialArtifactRepository,
    private val userRepository: UserRepository,
    private val encryptionService: MaterialEncryptionService,
    private val properties: MaterialEncryptionProperties,
) {
    @Transactional
    fun store(
        ownerId: UUID,
        kind: MaterialKind,
        mediaType: String,
        content: ByteArray,
        claimedSha256: String = sha256(content),
        extractionSha256: String? = null,
        rendererFingerprint: String? = null,
    ): StoredMaterialArtifact {
        validate(kind, mediaType, content, claimedSha256)
        artifactRepository.findByUserIdAndKindAndPlaintextSha256(ownerId, kind, claimedSha256)?.let {
            return it.summary()
        }

        val owner = userRepository.findById(ownerId).orElseThrow { NotFoundException("Owner not found") }
        val artifactId = UUID.randomUUID()
        return artifactRepository
            .saveAndFlush(
                ApplicationMaterialArtifactEntity(
                    id = artifactId,
                    user = owner,
                    kind = kind,
                    mediaType = mediaType,
                    encryptedContent = encryptionService.encrypt(ownerId, artifactId, kind.name, content),
                    plaintextSha256 = claimedSha256,
                    extractionSha256 = extractionSha256,
                    byteSize = content.size.toLong(),
                    rendererFingerprint = rendererFingerprint,
                ),
            ).summary()
    }

    fun read(ownerId: UUID, artifactId: UUID): ByteArray {
        val artifact =
            artifactRepository.findByIdAndUserId(artifactId, ownerId)
                ?: throw NotFoundException("Application material artifact not found")
        return encryptionService.decrypt(ownerId, artifact.id, artifact.kind.name, artifact.encryptedContent)
    }

    private fun validate(
        kind: MaterialKind,
        mediaType: String,
        content: ByteArray,
        claimedSha256: String,
    ) {
        if (content.isEmpty() || content.size > properties.maxArtifactBytes) {
            throw ValidationException("Artifact size is outside the allowed range")
        }
        if (mediaType !in ALLOWED_MEDIA_TYPES.getValue(kind)) {
            throw ValidationException("Unsupported media type for $kind")
        }
        if (!SHA256_PATTERN.matches(claimedSha256) || sha256(content) != claimedSha256) {
            throw ValidationException("Artifact SHA-256 does not match its content")
        }
    }

    private fun ApplicationMaterialArtifactEntity.summary() =
        StoredMaterialArtifact(id, kind, mediaType, plaintextSha256, byteSize)

    companion object {
        private val SHA256_PATTERN = Regex("[0-9a-f]{64}")
        private val ALLOWED_MEDIA_TYPES =
            mapOf(
                MaterialKind.CV_PDF to setOf("application/pdf"),
                MaterialKind.CV_DOCX to
                    setOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                MaterialKind.COVER_LETTER to setOf("text/plain", "text/plain;charset=UTF-8"),
                MaterialKind.RECRUITER_MESSAGE to setOf("text/plain", "text/plain;charset=UTF-8"),
            )

        fun sha256(content: ByteArray): String =
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content))
    }
}
