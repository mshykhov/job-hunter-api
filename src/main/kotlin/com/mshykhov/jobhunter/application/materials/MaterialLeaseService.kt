package com.mshykhov.jobhunter.application.materials

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.mshykhov.jobhunter.application.common.ConflictException
import com.mshykhov.jobhunter.application.common.NotFoundException
import com.mshykhov.jobhunter.application.common.ValidationException
import com.mshykhov.jobhunter.infrastructure.materials.EncryptedMaterialStore
import com.mshykhov.jobhunter.infrastructure.materials.MaterialEncryptionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class MaterialLeaseService(
    private val facade: ApplicationMaterialFacade,
    private val profileFacade: CandidateProfileFacade,
    private val stateMachine: MaterialStateMachine,
    private val encryptionService: MaterialEncryptionService,
    private val artifactStore: EncryptedMaterialStore,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {
    @Transactional
    fun claim(workerId: String): MaterialClaim? {
        val now = Instant.now(clock)
        facade.recoverExpired(now)
        val request = facade.claimNext() ?: return null
        stateMachine.requireTransition(request.status, MaterialStatus.CLAIMED)
        request.status = MaterialStatus.CLAIMED
        request.leaseOwner = workerId
        request.leaseToken = UUID.randomUUID()
        request.leaseExpiresAt = now.plus(LEASE_DURATION)
        request.attemptCount += 1
        facade.saveRequest(request)
        val ownerId = request.materialPackage.user.id
        return MaterialClaim(
            requestId = request.id,
            leaseToken = request.leaseToken!!,
            leaseExpiresAt = request.leaseExpiresAt!!,
            vacancy =
            decryptJson(
                ownerId,
                request.jobDescriptionVersion.id,
                "JOB_DESCRIPTION_NORMALIZED",
                request.jobDescriptionVersion.encryptedNormalizedContent,
            ),
            candidateProfile = decryptJson(ownerId, request.profileVersion.id, "CANDIDATE_PROFILE", request.profileVersion.encryptedContent),
            factCatalog = decryptJson(ownerId, request.factCatalogVersion.id, "FACT_CATALOG", request.factCatalogVersion.encryptedContent),
            writingStyle = decryptJson(ownerId, request.writingStyleVersion.id, "WRITING_STYLE", request.writingStyleVersion.encryptedContent),
            requestedKinds = request.requestedKinds,
            coverLetterPolicy = request.coverLetterPolicy,
            mode = request.requestMode,
            route = if (request.requestMode == MaterialRequestMode.SOL_IMPROVE) "SOL_IMPROVE" else "TERRA",
        )
    }

    @Transactional
    fun heartbeat(requestId: UUID, leaseToken: UUID): Instant {
        val request = requireLease(requestId, leaseToken)
        if (request.status == MaterialStatus.CLAIMED) {
            stateMachine.requireTransition(request.status, MaterialStatus.GENERATING)
            request.status = MaterialStatus.GENERATING
        }
        return Instant.now(clock).plus(LEASE_DURATION).also {
            request.leaseExpiresAt = it
            facade.saveRequest(request)
        }
    }

    @Transactional
    fun fail(requestId: UUID, leaseToken: UUID, retryable: Boolean): MaterialStatus {
        val request = requireLease(requestId, leaseToken)
        val target = if (retryable && request.attemptCount < MAX_ATTEMPTS) MaterialStatus.QUEUED else MaterialStatus.FAILED
        stateMachine.requireTransition(request.status, target)
        request.status = target
        clearLease(request)
        facade.saveRequest(request)
        return target
    }

    @Transactional
    fun complete(requestId: UUID, leaseToken: UUID, completion: MaterialCompletion): ApplicationMaterialRevisionEntity {
        facade.findRevisionByRequest(requestId)?.let { return it }
        val request = requireLease(requestId, leaseToken)
        validateCompletion(request, completion)
        advanceForCompletion(request, completion.status)
        val ownerId = request.materialPackage.user.id
        val stored =
            completion.artifacts.mapValues { (kind, upload) ->
                artifactStore.store(ownerId, kind, upload.mediaType, upload.content, upload.sha256, rendererFingerprint = completion.rendererVersion)
            }
        val revision =
            facade.saveRevision(
                ApplicationMaterialRevisionEntity(
                    materialPackage = request.materialPackage,
                    request = request,
                    revisionNumber = facade.nextRevisionNumber(request.materialPackage.id),
                    parentRevisionId = request.parentRevisionId,
                    origin = completion.origin,
                    inputSha256 = request.inputSha256,
                    generatorModel = completion.generatorModel,
                    rendererVersion = completion.rendererVersion,
                    eligibilityState = completion.status.name,
                    manifest = completion.manifest,
                ),
            )
        facade.saveRevisionArtifacts(
            stored.map { (kind, artifact) ->
                ApplicationMaterialRevisionArtifactEntity(revision, profileFacade.artifactReference(artifact.id), kind)
            } + inheritedArtifacts(request, revision, stored.keys),
        )
        request.status = completion.status
        clearLease(request)
        if (completion.status in ELIGIBLE_STATUSES) {
            request.materialPackage.selectedRevisionId = revision.id
            facade.savePackage(request.materialPackage)
        }
        facade.saveRequest(request)
        return revision
    }

    private fun requireLease(requestId: UUID, leaseToken: UUID): ApplicationMaterialRequestEntity {
        val request = facade.findRequestForUpdate(requestId) ?: throw NotFoundException("Material request not found")
        if (request.leaseToken != leaseToken || request.leaseExpiresAt?.isAfter(Instant.now(clock)) != true) {
            throw ConflictException("Material lease is stale or invalid")
        }
        return request
    }

    private fun validateCompletion(request: ApplicationMaterialRequestEntity, completion: MaterialCompletion) {
        if (completion.status !in TERMINAL_COMPLETION_STATUSES) throw ValidationException("Unsupported completion status")
        if (completion.status in ELIGIBLE_STATUSES && !completion.artifacts.keys.containsAll(request.requestedKinds)) {
            throw ValidationException("Eligible completion must contain every requested material")
        }
        if (request.coverLetterPolicy != CoverLetterPolicy.OPTIONAL_STANDARD &&
            completion.status in ELIGIBLE_STATUSES &&
            MaterialKind.COVER_LETTER !in completion.artifacts
        ) {
            throw ValidationException("Required cover letter is missing")
        }
        if (completion.artifacts.keys.any { it !in request.requestedKinds }) {
            throw ValidationException("Completion contains an unrequested artifact")
        }
    }

    private fun inheritedArtifacts(
        request: ApplicationMaterialRequestEntity,
        revision: ApplicationMaterialRevisionEntity,
        generatedKinds: Set<MaterialKind>,
    ): List<ApplicationMaterialRevisionArtifactEntity> =
        request.parentRevisionId
            ?.let(facade::findRevisionArtifacts)
            .orEmpty()
            .filter { it.kind !in generatedKinds }
            .map { ApplicationMaterialRevisionArtifactEntity(revision, it.artifact, it.kind) }

    private fun advanceForCompletion(request: ApplicationMaterialRequestEntity, target: MaterialStatus) {
        if (request.status == MaterialStatus.CLAIMED) {
            stateMachine.requireTransition(request.status, MaterialStatus.GENERATING)
            request.status = MaterialStatus.GENERATING
        }
        if (target == MaterialStatus.READY) {
            stateMachine.requireTransition(request.status, MaterialStatus.VALIDATING)
            request.status = MaterialStatus.VALIDATING
            stateMachine.requireTransition(request.status, MaterialStatus.RENDERING)
            request.status = MaterialStatus.RENDERING
        }
        stateMachine.requireTransition(request.status, target)
    }

    private fun clearLease(request: ApplicationMaterialRequestEntity) {
        request.leaseOwner = null
        request.leaseToken = null
        request.leaseExpiresAt = null
    }

    private fun decryptJson(ownerId: UUID, recordId: UUID, kind: String, encrypted: ByteArray): JsonNode =
        objectMapper.readTree(encryptionService.decrypt(ownerId, recordId, kind, encrypted))

    private companion object {
        val LEASE_DURATION: Duration = Duration.ofMinutes(10)
        const val MAX_ATTEMPTS = 3
        val ELIGIBLE_STATUSES = setOf(MaterialStatus.READY, MaterialStatus.READY_WITH_FALLBACK)
        val TERMINAL_COMPLETION_STATUSES = ELIGIBLE_STATUSES + MaterialStatus.BLOCKED
    }
}
