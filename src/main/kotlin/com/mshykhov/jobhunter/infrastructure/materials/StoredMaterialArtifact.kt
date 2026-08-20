package com.mshykhov.jobhunter.infrastructure.materials

import com.mshykhov.jobhunter.application.materials.MaterialKind
import java.util.UUID

data class StoredMaterialArtifact(val id: UUID, val kind: MaterialKind, val mediaType: String, val plaintextSha256: String, val byteSize: Long)
