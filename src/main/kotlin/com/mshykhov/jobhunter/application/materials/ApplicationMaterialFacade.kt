package com.mshykhov.jobhunter.application.materials

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Transactional(readOnly = true)
class ApplicationMaterialFacade(
    private val packageRepository: ApplicationMaterialPackageRepository,
    private val requestRepository: ApplicationMaterialRequestRepository,
    private val jobDescriptionRepository: JobDescriptionVersionRepository,
    private val revisionRepository: ApplicationMaterialRevisionRepository,
    private val revisionArtifactRepository: ApplicationMaterialRevisionArtifactRepository,
) {
    fun findPackage(userId: UUID, jobId: UUID) = packageRepository.findByUserIdAndJobId(userId, jobId)
    fun findRequest(packageId: UUID, idempotencyKey: String) = requestRepository.findByMaterialPackageIdAndIdempotencyKey(packageId, idempotencyKey)
    fun findRequests(packageId: UUID) = requestRepository.findAllByMaterialPackageIdOrderByCreatedAtDesc(packageId)
    fun findRequestForUpdate(id: UUID) = requestRepository.findForUpdate(id)
    fun claimNext() = requestRepository.claimNext()
    fun findRevisionByRequest(requestId: UUID) = revisionRepository.findByRequestId(requestId)
    fun findRevisions(packageId: UUID) = revisionRepository.findAllByMaterialPackageIdOrderByRevisionNumberDesc(packageId)
    fun findRevision(userId: UUID, revisionId: UUID) = revisionRepository.findByIdAndMaterialPackageUserId(revisionId, userId)
    fun nextRevisionNumber(packageId: UUID) = revisionRepository.findMaxRevisionNumber(packageId) + 1
    fun findRevisionArtifacts(revisionId: UUID) = revisionArtifactRepository.findAllByRevisionId(revisionId)
    fun findRevisionArtifact(revisionId: UUID, kind: MaterialKind) = revisionArtifactRepository.findByRevisionIdAndKind(revisionId, kind)
    fun findJobDescription(userId: UUID, jobId: UUID, hash: String) =
        jobDescriptionRepository.findByUserIdAndJobIdAndContentSha256(userId, jobId, hash)

    @Transactional fun savePackage(entity: ApplicationMaterialPackageEntity) = packageRepository.save(entity)

    @Transactional fun saveRequest(entity: ApplicationMaterialRequestEntity) = requestRepository.save(entity)

    @Transactional fun saveJobDescription(entity: JobDescriptionVersionEntity) = jobDescriptionRepository.save(entity)

    @Transactional fun saveRevision(entity: ApplicationMaterialRevisionEntity) = revisionRepository.save(entity)

    @Transactional fun saveRevisionArtifacts(entities: List<ApplicationMaterialRevisionArtifactEntity>) = revisionArtifactRepository.saveAll(entities)

    @Transactional
    fun recoverExpired(now: java.time.Instant) {
        requestRepository.failExhaustedExpired(now)
        requestRepository.requeueExpired(now)
    }
}
