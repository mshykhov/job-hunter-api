package com.mshykhov.jobhunter.application.materials

import java.time.Instant
import java.util.UUID

data class ImportedCandidateProfile(
    val id: UUID,
    val profileVersion: String,
    val schemaVersion: String,
    val sourceCommit: String,
    val active: Boolean,
    val createdAt: Instant?,
)
