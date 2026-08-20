package com.mshykhov.jobhunter.application.materials

data class DownloadedMaterialArtifact(val kind: MaterialKind, val mediaType: String, val filename: String, val content: ByteArray)
