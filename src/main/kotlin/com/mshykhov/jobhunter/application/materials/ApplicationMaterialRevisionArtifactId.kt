package com.mshykhov.jobhunter.application.materials

import java.io.Serializable
import java.util.UUID

data class ApplicationMaterialRevisionArtifactId(val revision: UUID = UUID.randomUUID(), val kind: MaterialKind = MaterialKind.CV_PDF) : Serializable
