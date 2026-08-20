package com.mshykhov.jobhunter.api.rest.materials.dto

import com.mshykhov.jobhunter.application.materials.CoverLetterPolicy
import com.mshykhov.jobhunter.application.materials.MaterialKind
import com.mshykhov.jobhunter.application.materials.MaterialRequestMode

data class CreateMaterialRequest(
    val requestedKinds: Set<MaterialKind> =
        setOf(MaterialKind.CV_DOCX, MaterialKind.CV_PDF, MaterialKind.COVER_LETTER, MaterialKind.RECRUITER_MESSAGE),
    val coverLetterPolicy: CoverLetterPolicy = CoverLetterPolicy.OPTIONAL_STANDARD,
    val mode: MaterialRequestMode = MaterialRequestMode.TERRA,
    val regenerate: Boolean = false,
)
