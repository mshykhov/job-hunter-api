package com.mshykhov.jobhunter.api.rest.automation.dto

import com.fasterxml.jackson.databind.JsonNode
import com.mshykhov.jobhunter.application.materials.CoverLetterPolicy
import com.mshykhov.jobhunter.application.materials.MaterialClaim
import com.mshykhov.jobhunter.application.materials.MaterialKind
import com.mshykhov.jobhunter.application.materials.MaterialRequestMode
import java.time.Instant
import java.util.UUID

data class AutomationMaterialClaimResponse(
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
) {
    companion object {
        fun from(claim: MaterialClaim) =
            AutomationMaterialClaimResponse(
                claim.requestId,
                claim.leaseToken,
                claim.leaseExpiresAt,
                claim.vacancy,
                claim.candidateProfile,
                claim.factCatalog,
                claim.writingStyle,
                claim.requestedKinds,
                claim.coverLetterPolicy,
                claim.mode,
                claim.route,
            )
    }
}
