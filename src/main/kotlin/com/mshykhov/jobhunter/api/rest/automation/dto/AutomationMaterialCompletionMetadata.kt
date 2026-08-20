package com.mshykhov.jobhunter.api.rest.automation.dto

import com.mshykhov.jobhunter.application.materials.MaterialKind
import com.mshykhov.jobhunter.application.materials.MaterialOrigin
import com.mshykhov.jobhunter.application.materials.MaterialStatus
import java.util.UUID

data class AutomationMaterialCompletionMetadata(
    val leaseToken: UUID,
    val status: MaterialStatus,
    val origin: MaterialOrigin,
    val generatorModel: String?,
    val rendererVersion: String,
    val manifest: Map<String, Any?>,
    val artifactSha256: Map<MaterialKind, String>,
)
