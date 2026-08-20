package com.mshykhov.jobhunter.application.materials

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Transactional(readOnly = true)
class CandidateProfileFacade(
    private val profileRepository: CandidateProfileVersionRepository,
    private val factCatalogRepository: FactCatalogVersionRepository,
    private val writingStyleRepository: WritingStyleVersionRepository,
    private val artifactRepository: ApplicationMaterialArtifactRepository,
) {
    fun findActiveProfile(userId: UUID) = profileRepository.findByUserIdAndActiveTrue(userId)
    fun findProfiles(userId: UUID) = profileRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
    fun findProfile(userId: UUID, profileVersion: String) = profileRepository.findByUserIdAndProfileVersion(userId, profileVersion)
    fun findFactCatalog(userId: UUID, contentSha256: String) = factCatalogRepository.findByUserIdAndContentSha256(userId, contentSha256)
    fun findWritingStyle(userId: UUID, contentSha256: String) = writingStyleRepository.findByUserIdAndContentSha256(userId, contentSha256)
    fun findActiveWritingStyle(userId: UUID) = writingStyleRepository.findByUserIdAndActiveTrue(userId)
    fun artifactReference(id: UUID) = artifactRepository.getReferenceById(id)

    @Transactional
    fun saveFactCatalog(entity: FactCatalogVersionEntity) = factCatalogRepository.save(entity)

    @Transactional
    fun activateWritingStyle(entity: WritingStyleVersionEntity): WritingStyleVersionEntity {
        writingStyleRepository.findByUserIdAndActiveTrue(entity.user.id)?.takeIf { it.id != entity.id }?.let {
            it.active = false
            writingStyleRepository.saveAndFlush(it)
        }
        entity.active = true
        return writingStyleRepository.save(entity)
    }

    @Transactional
    fun activateProfile(entity: CandidateProfileVersionEntity): CandidateProfileVersionEntity {
        profileRepository.findByUserIdAndActiveTrue(entity.user.id)?.takeIf { it.id != entity.id }?.let {
            it.active = false
            profileRepository.saveAndFlush(it)
        }
        entity.active = true
        return profileRepository.save(entity)
    }
}
