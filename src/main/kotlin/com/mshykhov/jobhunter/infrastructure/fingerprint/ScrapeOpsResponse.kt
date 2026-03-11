package com.mshykhov.jobhunter.infrastructure.fingerprint

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ScrapeOpsResponse(val result: List<BrowserFingerprint> = emptyList())
