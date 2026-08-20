package com.mshykhov.jobhunter.application.materials

import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.util.UUID

data class MaterialClaim(
    val requestId: UUID,
    val leaseToken: UUID,
    val leaseExpiresAt: Instant,
    val vacancy: JsonNode,
    val candidateProfile: JsonNode,
    val factCatalog: JsonNode,
    val writingStyle: JsonNode,
    val requestedKinds: Set<MaterialKind>,
    val coverLetterPolicy: CoverLetterPolicy,
    val mode: MaterialRequestMode,
    val route: String,
)
