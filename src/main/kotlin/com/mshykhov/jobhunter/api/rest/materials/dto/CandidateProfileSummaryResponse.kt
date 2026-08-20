package com.mshykhov.jobhunter.api.rest.materials.dto

import com.mshykhov.jobhunter.application.materials.ImportedCandidateProfile
import java.time.Instant
import java.util.UUID

data class CandidateProfileSummaryResponse(
    val id: UUID,
    val profileVersion: String,
    val schemaVersion: String,
    val sourceCommit: String,
    val active: Boolean,
    val createdAt: Instant?,
) {
    companion object {
        fun from(profile: ImportedCandidateProfile) =
            CandidateProfileSummaryResponse(
                profile.id,
                profile.profileVersion,
                profile.schemaVersion,
                profile.sourceCommit,
                profile.active,
                profile.createdAt,
            )
    }
}
