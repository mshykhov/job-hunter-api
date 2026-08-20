package com.mshykhov.jobhunter.application.materials

import java.time.Instant
import java.util.UUID

data class ApplicationMaterialRevisionView(
    val id: UUID,
    val revisionNumber: Int,
    val parentRevisionId: UUID?,
    val origin: MaterialOrigin,
    val generatorModel: String?,
    val rendererVersion: String,
    val eligibilityState: String,
    val selected: Boolean,
    val artifacts: List<ApplicationMaterialArtifactView>,
    val createdAt: Instant?,
)

data class ApplicationMaterialArtifactView(val kind: MaterialKind, val mediaType: String, val sha256: String, val byteSize: Long)
