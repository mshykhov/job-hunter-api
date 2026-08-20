package com.mshykhov.jobhunter.api.rest.materials.dto

import com.mshykhov.jobhunter.application.materials.ApplicationMaterialRequestView
import com.mshykhov.jobhunter.application.materials.CoverLetterPolicy
import com.mshykhov.jobhunter.application.materials.MaterialKind
import com.mshykhov.jobhunter.application.materials.MaterialRequestMode
import com.mshykhov.jobhunter.application.materials.MaterialStatus
import java.time.Instant
import java.util.UUID

data class ApplicationMaterialRequestResponse(
    val packageId: UUID,
    val requestId: UUID,
    val status: MaterialStatus,
    val mode: MaterialRequestMode,
    val requestedKinds: Set<MaterialKind>,
    val coverLetterPolicy: CoverLetterPolicy,
    val createdAt: Instant?,
    val updatedAt: Instant?,
) {
    companion object {
        fun from(view: ApplicationMaterialRequestView) =
            ApplicationMaterialRequestResponse(
                view.packageId,
                view.requestId,
                view.status,
                view.mode,
                view.requestedKinds,
                view.coverLetterPolicy,
                view.createdAt,
                view.updatedAt,
            )
    }
}
