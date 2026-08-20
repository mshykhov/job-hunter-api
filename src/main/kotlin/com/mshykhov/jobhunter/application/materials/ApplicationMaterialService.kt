package com.mshykhov.jobhunter.application.materials

import com.fasterxml.jackson.databind.ObjectMapper
import com.mshykhov.jobhunter.application.common.NotFoundException
import com.mshykhov.jobhunter.application.common.ValidationException
import com.mshykhov.jobhunter.application.user.UserEntity
import com.mshykhov.jobhunter.application.user.UserFacade
import com.mshykhov.jobhunter.application.userjob.UserJobFacade
import com.mshykhov.jobhunter.infrastructure.materials.EncryptedMaterialStore
import com.mshykhov.jobhunter.infrastructure.materials.MaterialEncryptionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class ApplicationMaterialService(
    private val userFacade: UserFacade,
    private val userJobFacade: UserJobFacade,
    private val profileFacade: CandidateProfileFacade,
    private val materialFacade: ApplicationMaterialFacade,
    private val encryptionService: MaterialEncryptionService,
    private val artifactStore: EncryptedMaterialStore,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {
    @Transactional
    fun ensureReady(
        subject: String,
        jobId: UUID,
        requestedKinds: Set<MaterialKind>,
        mode: MaterialRequestMode,
        coverLetterPolicy: CoverLetterPolicy,
        regenerate: Boolean,
        parentRevisionId: UUID? = null,
    ): ApplicationMaterialRequestView {
        validateRequestedKinds(requestedKinds, coverLetterPolicy)
        val user = userFacade.findOrCreate(subject)
        val userJob = userJobFacade.findOrCreateForGroupMember(user, jobId)
        val profile = profileFacade.findActiveProfile(user.id) ?: throw ValidationException("Import an active candidate profile first")
        val writingStyle = profileFacade.findActiveWritingStyle(user.id) ?: throw ValidationException("Import an active writing style first")
        val jobDescription = snapshotJob(user, userJob.job)
        val materialPackage =
            materialFacade.findPackage(user.id, jobId)
                ?: materialFacade.savePackage(ApplicationMaterialPackageEntity(user = user, job = userJob.job))
        val input =
            listOf(
                jobDescription.contentSha256,
                profile.profileVersion,
                profile.factCatalogVersion.contentSha256,
                writingStyle.contentSha256,
                requestedKinds.map(MaterialKind::name).sorted().joinToString(","),
                coverLetterPolicy.name,
                mode.name,
            ).joinToString("|")
        val inputSha256 = EncryptedMaterialStore.sha256(input.toByteArray(StandardCharsets.UTF_8))
        val idempotencyKey = if (regenerate) "$inputSha256:${UUID.randomUUID()}" else inputSha256
        if (!regenerate) {
            materialFacade.findRequest(materialPackage.id, idempotencyKey)?.let { return it.view() }
        }
        return materialFacade.saveRequest(
            ApplicationMaterialRequestEntity(
                materialPackage = materialPackage,
                jobDescriptionVersion = jobDescription,
                profileVersion = profile,
                factCatalogVersion = profile.factCatalogVersion,
                writingStyleVersion = writingStyle,
                status = MaterialStatus.QUEUED,
                requestMode = mode,
                requestedKinds = requestedKinds,
                coverLetterPolicy = coverLetterPolicy,
                generationPolicyVersion = GENERATION_POLICY_VERSION,
                schemaVersion = SCHEMA_VERSION,
                rendererVersion = RENDERER_VERSION,
                modelRoute = if (mode == MaterialRequestMode.SOL_IMPROVE) "SOL" else "TERRA",
                inputSha256 = inputSha256,
                idempotencyKey = idempotencyKey,
                parentRevisionId = parentRevisionId,
            ),
        ).view()
    }

    @Transactional(readOnly = true)
    fun findForJob(subject: String, jobId: UUID): List<ApplicationMaterialRequestView> {
        val user = userFacade.findByAuth0Sub(subject) ?: return emptyList()
        val materialPackage = materialFacade.findPackage(user.id, jobId) ?: return emptyList()
        return materialFacade.findRequests(materialPackage.id).map { it.view() }
    }

    @Transactional(readOnly = true)
    fun findRevisions(subject: String, jobId: UUID): List<ApplicationMaterialRevisionView> {
        val user = userFacade.findByAuth0Sub(subject) ?: return emptyList()
        val materialPackage = materialFacade.findPackage(user.id, jobId) ?: return emptyList()
        return materialFacade.findRevisions(materialPackage.id).map { revision ->
            revision.view(
                selected = materialPackage.selectedRevisionId == revision.id,
                artifacts = materialFacade.findRevisionArtifacts(revision.id),
            )
        }
    }

    @Transactional
    fun improveWithSol(subject: String, revisionId: UUID): ApplicationMaterialRequestView {
        val user = userFacade.findByAuth0Sub(subject) ?: throw NotFoundException("Material revision not found")
        val revision = materialFacade.findRevision(user.id, revisionId) ?: throw NotFoundException("Material revision not found")
        return ensureReady(
            subject,
            revision.materialPackage.job.id,
            revision.request.requestedKinds,
            MaterialRequestMode.SOL_IMPROVE,
            revision.request.coverLetterPolicy,
            regenerate = true,
            parentRevisionId = revision.id,
        )
    }

    @Transactional
    fun selectRevision(subject: String, revisionId: UUID) {
        val user = userFacade.findByAuth0Sub(subject) ?: throw NotFoundException("Material revision not found")
        val revision = materialFacade.findRevision(user.id, revisionId) ?: throw NotFoundException("Material revision not found")
        if (revision.eligibilityState !in setOf(MaterialStatus.READY.name, MaterialStatus.READY_WITH_FALLBACK.name)) {
            throw ValidationException("Only an eligible revision can be selected")
        }
        revision.materialPackage.selectedRevisionId = revision.id
        materialFacade.savePackage(revision.materialPackage)
    }

    @Transactional(readOnly = true)
    fun downloadArtifact(subject: String, revisionId: UUID, kind: MaterialKind): DownloadedMaterialArtifact {
        val user = userFacade.findByAuth0Sub(subject) ?: throw NotFoundException("Material artifact not found")
        materialFacade.findRevision(user.id, revisionId) ?: throw NotFoundException("Material artifact not found")
        val link = materialFacade.findRevisionArtifact(revisionId, kind) ?: throw NotFoundException("Material artifact not found")
        return DownloadedMaterialArtifact(
            kind,
            link.artifact.mediaType,
            filename(kind),
            artifactStore.read(user.id, link.artifact.id),
        )
    }

    private fun snapshotJob(owner: UserEntity, job: com.mshykhov.jobhunter.application.job.JobEntity): JobDescriptionVersionEntity {
        val snapshot =
            linkedMapOf(
                "id" to job.id,
                "title" to job.title,
                "company" to job.company,
                "description" to job.description,
                "location" to job.location,
                "salary" to job.salary,
                "remote" to job.remote,
                "url" to job.url,
                "source" to job.source.name,
            )
        val normalized = objectMapper.writeValueAsBytes(snapshot)
        val hash = EncryptedMaterialStore.sha256(normalized)
        materialFacade.findJobDescription(owner.id, job.id, hash)?.let { return it }
        val id = UUID.randomUUID()
        return materialFacade.saveJobDescription(
            JobDescriptionVersionEntity(
                id = id,
                user = owner,
                job = job,
                contentSha256 = hash,
                encryptedRawContent = encryptionService.encrypt(owner.id, id, "JOB_DESCRIPTION_RAW", job.description.toByteArray()),
                encryptedNormalizedContent = encryptionService.encrypt(owner.id, id, "JOB_DESCRIPTION_NORMALIZED", normalized),
                parserVersion = "job-snapshot/1",
                capturedAt = Instant.now(clock),
            ),
        )
    }

    private fun validateRequestedKinds(kinds: Set<MaterialKind>, policy: CoverLetterPolicy) {
        if (!kinds.containsAll(setOf(MaterialKind.CV_DOCX, MaterialKind.CV_PDF))) {
            throw ValidationException("Every package must include CV DOCX and PDF")
        }
        if (policy != CoverLetterPolicy.OPTIONAL_STANDARD && MaterialKind.COVER_LETTER !in kinds) {
            throw ValidationException("Required cover-letter policy needs a cover letter")
        }
    }

    private fun ApplicationMaterialRequestEntity.view() =
        ApplicationMaterialRequestView(
            materialPackage.id,
            id,
            status,
            requestMode,
            requestedKinds,
            coverLetterPolicy,
            createdAt,
            updatedAt,
        )

    private fun ApplicationMaterialRevisionEntity.view(
        selected: Boolean,
        artifacts: List<ApplicationMaterialRevisionArtifactEntity>,
    ) =
        ApplicationMaterialRevisionView(
            id,
            revisionNumber,
            parentRevisionId,
            origin,
            generatorModel,
            rendererVersion,
            eligibilityState,
            selected,
            artifacts.map {
                ApplicationMaterialArtifactView(it.kind, it.artifact.mediaType, it.artifact.plaintextSha256, it.artifact.byteSize)
            },
            createdAt,
        )

    private fun filename(kind: MaterialKind) =
        when (kind) {
            MaterialKind.CV_DOCX -> "cv.docx"
            MaterialKind.CV_PDF -> "cv.pdf"
            MaterialKind.COVER_LETTER -> "cover-letter.txt"
            MaterialKind.RECRUITER_MESSAGE -> "recruiter-message.txt"
        }

    private companion object {
        const val SCHEMA_VERSION = "application-materials/v1"
        const val GENERATION_POLICY_VERSION = "materials-policy/1"
        const val RENDERER_VERSION = "cv-materials/0.1.0"
    }
}
