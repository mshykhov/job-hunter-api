package com.mshykhov.jobhunter.api.rest.materials.dto

import com.mshykhov.jobhunter.application.materials.ApplicationMaterialArtifactView
import com.mshykhov.jobhunter.application.materials.ApplicationMaterialRevisionView
import com.mshykhov.jobhunter.application.materials.MaterialOrigin
import java.time.Instant
import java.util.UUID

data class ApplicationMaterialRevisionResponse(
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
) {
    companion object {
        fun from(view: ApplicationMaterialRevisionView) =
            ApplicationMaterialRevisionResponse(
                view.id,
                view.revisionNumber,
                view.parentRevisionId,
                view.origin,
                view.generatorModel,
                view.rendererVersion,
                view.eligibilityState,
                view.selected,
                view.artifacts,
                view.createdAt,
            )
    }
}
