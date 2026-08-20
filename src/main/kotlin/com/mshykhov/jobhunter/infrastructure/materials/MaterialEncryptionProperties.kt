package com.mshykhov.jobhunter.infrastructure.materials

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jobhunter.materials")
data class MaterialEncryptionProperties(val encryptionKey: String = "", val maxArtifactBytes: Long = 5_242_880)
