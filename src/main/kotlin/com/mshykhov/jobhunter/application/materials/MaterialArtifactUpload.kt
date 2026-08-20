package com.mshykhov.jobhunter.application.materials

data class MaterialArtifactUpload(val content: ByteArray, val mediaType: String, val sha256: String)
