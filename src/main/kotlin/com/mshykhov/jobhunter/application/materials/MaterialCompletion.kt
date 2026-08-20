package com.mshykhov.jobhunter.application.materials

data class MaterialCompletion(
    val status: MaterialStatus,
    val origin: MaterialOrigin,
    val generatorModel: String?,
    val rendererVersion: String,
    val manifest: Map<String, Any?>,
    val artifacts: Map<MaterialKind, MaterialArtifactUpload>,
)
