package com.mshykhov.jobhunter.application.materials

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.mshykhov.jobhunter.application.common.ValidationException
import com.mshykhov.jobhunter.application.user.UserFacade
import com.mshykhov.jobhunter.infrastructure.materials.EncryptedMaterialStore
import com.mshykhov.jobhunter.infrastructure.materials.MaterialEncryptionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.util.HexFormat
import java.util.TreeMap
import java.util.UUID

@Service
class CandidateProfileService(
    private val userFacade: UserFacade,
    private val facade: CandidateProfileFacade,
    private val artifactStore: EncryptedMaterialStore,
    private val encryptionService: MaterialEncryptionService,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun importProfile(
        subject: String,
        manifestBytes: ByteArray,
        candidateProfileBytes: ByteArray,
        factCatalogBytes: ByteArray,
        writingStyleBytes: ByteArray,
        baseDocx: ByteArray,
        basePdf: ByteArray,
    ): ImportedCandidateProfile {
        val user = userFacade.findOrCreate(subject)
        val manifest = parseObject(manifestBytes, "manifest")
        val profile = parseObject(candidateProfileBytes, "candidate profile")
        val factCatalog = parseObject(factCatalogBytes, "fact catalog")
        val writingStyle = parseObject(writingStyleBytes, "writing style")
        validateBundle(manifest, profile, factCatalog, writingStyle, baseDocx, basePdf)

        val profileVersion = manifest.requiredText("profileVersion")
        facade.findProfile(user.id, profileVersion)?.let { return facade.activateProfile(it).summary() }

        val factHash = manifest.requiredText("factCatalogVersion")
        val factCatalogVersion =
            facade.findFactCatalog(user.id, factHash) ?: UUID.randomUUID().let { id ->
                facade.saveFactCatalog(
                    FactCatalogVersionEntity(
                        id = id,
                        user = user,
                        schemaVersion = SCHEMA_VERSION,
                        contentSha256 = factHash,
                        encryptedContent = encryptionService.encrypt(user.id, id, "FACT_CATALOG", canonicalBytes(factCatalog)),
                    ),
                )
            }

        val writingHash = manifest.requiredText("writingStyleVersion")
        val writingStyleVersion =
            facade.findWritingStyle(user.id, writingHash) ?: UUID.randomUUID().let { id ->
                WritingStyleVersionEntity(
                    id = id,
                    user = user,
                    contentSha256 = writingHash,
                    encryptedContent = encryptionService.encrypt(user.id, id, "WRITING_STYLE", canonicalBytes(writingStyle)),
                )
            }
        facade.activateWritingStyle(writingStyleVersion)

        val docx = artifactStore.store(user.id, MaterialKind.CV_DOCX, DOCX_MEDIA_TYPE, baseDocx)
        val pdf = artifactStore.store(user.id, MaterialKind.CV_PDF, "application/pdf", basePdf)
        val profileId = UUID.randomUUID()
        return facade.activateProfile(
            CandidateProfileVersionEntity(
                id = profileId,
                user = user,
                schemaVersion = SCHEMA_VERSION,
                profileVersion = profileVersion,
                contentSha256 = profileVersion,
                encryptedContent = encryptionService.encrypt(user.id, profileId, "CANDIDATE_PROFILE", canonicalBytes(profile)),
                factCatalogVersion = factCatalogVersion,
                baseDocxArtifact = facade.artifactReference(docx.id),
                basePdfArtifact = facade.artifactReference(pdf.id),
                validationMetadata =
                mapOf(
                    "pageCount" to manifest.path("pageCount").asInt(),
                    "validationCodes" to manifest.path("validationCodes").map(JsonNode::asText),
                ),
                sourceCommit = manifest.requiredText("sourceCommit"),
            ),
        ).summary()
    }

    @Transactional(readOnly = true)
    fun listProfiles(subject: String): List<ImportedCandidateProfile> {
        val user = userFacade.findOrCreate(subject)
        return facade.findProfiles(user.id).map { it.summary() }
    }

    private fun validateBundle(
        manifest: JsonNode,
        profile: JsonNode,
        factCatalog: JsonNode,
        writingStyle: JsonNode,
        baseDocx: ByteArray,
        basePdf: ByteArray,
    ) {
        if (manifest.requiredText("schemaVersion") != SCHEMA_VERSION || profile.requiredText("schemaVersion") != SCHEMA_VERSION) {
            throw ValidationException("Unsupported application-materials schema version")
        }
        requireHash(manifest.requiredText("profileVersion"), canonicalBytes(profile), "candidate profile")
        requireHash(manifest.requiredText("factCatalogVersion"), canonicalBytes(factCatalog), "fact catalog")
        requireHash(manifest.requiredText("writingStyleVersion"), canonicalBytes(writingStyle), "writing style")
        if (profile.requiredText("factCatalogVersion") != manifest.requiredText("factCatalogVersion")) {
            throw ValidationException("Candidate profile references a different fact catalog")
        }
        if (factCatalog.requiredText("schemaVersion") != SCHEMA_VERSION || writingStyle.requiredText("schemaVersion") != SCHEMA_VERSION) {
            throw ValidationException("Bundle documents use inconsistent schema versions")
        }
        requireHash(manifest.path("baseArtifacts").requiredText("docxSha256"), baseDocx, "base DOCX")
        requireHash(manifest.path("baseArtifacts").requiredText("pdfSha256"), basePdf, "base PDF")
        if (manifest.path("pageCount").asInt(0) !in 1..2) throw ValidationException("Base CV must contain one or two pages")
        val forbidden = profile.path("forbiddenRenderFields").map(JsonNode::asText).toSet()
        if (forbidden != REQUIRED_FORBIDDEN_FIELDS) throw ValidationException("Candidate profile render restrictions are incomplete")
        val privateFacts = profile.path("privateMatchingFacts")
        if (privateFacts.path("englishLevel").asText() != "C1" || privateFacts.path("renderable").asBoolean(true)) {
            throw ValidationException("English C1 must remain a private non-renderable matching fact")
        }
    }

    private fun parseObject(bytes: ByteArray, label: String): JsonNode =
        try {
            objectMapper.readTree(bytes).takeIf(JsonNode::isObject) ?: throw ValidationException("$label must be a JSON object")
        } catch (exception: ValidationException) {
            throw exception
        } catch (exception: Exception) {
            throw ValidationException("$label is not valid JSON")
        }

    private fun canonicalBytes(node: JsonNode): ByteArray = objectMapper.writeValueAsBytes(canonicalValue(node))

    private fun canonicalValue(node: JsonNode): Any? =
        when {
            node.isObject -> TreeMap<String, Any?>().also { result -> node.fields().forEach { result[it.key] = canonicalValue(it.value) } }
            node.isArray -> node.map(::canonicalValue)
            node.isTextual -> node.textValue()
            node.isIntegralNumber -> node.longValue()
            node.isFloatingPointNumber -> node.decimalValue()
            node.isBoolean -> node.booleanValue()
            node.isNull -> null
            else -> throw ValidationException("Unsupported JSON value")
        }

    private fun JsonNode.requiredText(field: String): String =
        path(field).takeIf { it.isTextual && it.asText().isNotBlank() }?.asText()
            ?: throw ValidationException("Missing $field")

    private fun requireHash(expected: String, content: ByteArray, label: String) {
        if (expected != sha256(content)) throw ValidationException("$label SHA-256 mismatch")
    }

    private fun CandidateProfileVersionEntity.summary() =
        ImportedCandidateProfile(id, profileVersion, schemaVersion, sourceCommit, active, createdAt)

    private companion object {
        const val SCHEMA_VERSION = "application-materials/v1"
        const val DOCX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        val REQUIRED_FORBIDDEN_FIELDS = setOf("age", "dateOfBirth", "education", "languages")
        fun sha256(content: ByteArray) = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content))
    }
}
