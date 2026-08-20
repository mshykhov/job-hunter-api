package com.mshykhov.jobhunter.application.materials

import java.time.Instant
import java.util.UUID

data class ApplicationMaterialRequestView(
    val packageId: UUID,
    val requestId: UUID,
    val status: MaterialStatus,
    val mode: MaterialRequestMode,
    val requestedKinds: Set<MaterialKind>,
    val coverLetterPolicy: CoverLetterPolicy,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)
