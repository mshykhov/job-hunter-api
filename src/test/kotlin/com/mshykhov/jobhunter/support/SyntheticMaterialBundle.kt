package com.mshykhov.jobhunter.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.mshykhov.jobhunter.infrastructure.materials.EncryptedMaterialStore

data class SyntheticMaterialBundle(
    val manifest: ByteArray,
    val candidateProfile: ByteArray,
    val factCatalog: ByteArray,
    val writingStyle: ByteArray,
    val baseDocx: ByteArray,
    val basePdf: ByteArray,
) {
    companion object {
        fun create(objectMapper: ObjectMapper): SyntheticMaterialBundle {
            val factCatalog =
                sortedMapOf<String, Any?>(
                    "experience" to emptyList<Any>(),
                    "qualifications" to emptyList<Any>(),
                    "schemaVersion" to "application-materials/v1",
                    "summary" to emptyList<Any>(),
                )
            val writingStyle =
                sortedMapOf<String, Any?>(
                    "examples" to emptyList<Any>(),
                    "schemaVersion" to "application-materials/v1",
                    "voiceRules" to listOf("Write direct C1 English."),
                )
            val factBytes = objectMapper.writeValueAsBytes(factCatalog)
            val writingBytes = objectMapper.writeValueAsBytes(writingStyle)
            val profile =
                sortedMapOf<String, Any?>(
                    "contacts" to emptyList<Any>(),
                    "experience" to emptyList<Any>(),
                    "factCatalogVersion" to EncryptedMaterialStore.sha256(factBytes),
                    "forbiddenRenderFields" to listOf("age", "dateOfBirth", "education", "languages"),
                    "identity" to sortedMapOf("name" to "Alex Example", "title" to "Backend Engineer"),
                    "metadata" to sortedMapOf("author" to "Alex Example"),
                    "privateMatchingFacts" to sortedMapOf("englishLevel" to "C1", "renderable" to false),
                    "protectedTerms" to emptyList<String>(),
                    "schemaVersion" to "application-materials/v1",
                )
            val profileBytes = objectMapper.writeValueAsBytes(profile)
            val docx = "synthetic docx".toByteArray()
            val pdf = "%PDF synthetic".toByteArray()
            val manifest =
                mapOf(
                    "schemaVersion" to "application-materials/v1",
                    "profileVersion" to EncryptedMaterialStore.sha256(profileBytes),
                    "factCatalogVersion" to EncryptedMaterialStore.sha256(factBytes),
                    "writingStyleVersion" to EncryptedMaterialStore.sha256(writingBytes),
                    "sourceCommit" to "0123456789abcdef",
                    "rendererVersion" to "cv-materials/test",
                    "pageCount" to 2,
                    "baseArtifacts" to
                        mapOf(
                            "docxSha256" to EncryptedMaterialStore.sha256(docx),
                            "pdfSha256" to EncryptedMaterialStore.sha256(pdf),
                        ),
                    "validationCodes" to listOf("OK"),
                )
            return SyntheticMaterialBundle(
                objectMapper.writeValueAsBytes(manifest),
                profileBytes,
                factBytes,
                writingBytes,
                docx,
                pdf,
            )
        }
    }
}
