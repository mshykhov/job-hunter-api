package com.mshykhov.jobhunter.infrastructure.fingerprint

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class BrowserFingerprint(
    @JsonProperty("user-agent") val userAgent: String? = null,
    @JsonProperty("accept-language") val acceptLanguage: String? = null,
    @JsonProperty("sec-ch-ua") val secChUa: String? = null,
    @JsonProperty("sec-ch-ua-mobile") val secChUaMobile: String? = null,
    @JsonProperty("sec-ch-ua-platform") val secChUaPlatform: String? = null,
) {
    fun toHeaderMap(): Map<String, String> =
        buildMap {
            userAgent?.let { put("User-Agent", it) }
            acceptLanguage?.let { put("Accept-Language", it) }
            secChUa?.let { put("Sec-Ch-Ua", it) }
            secChUaMobile?.let { put("Sec-Ch-Ua-Mobile", it) }
            secChUaPlatform?.let { put("Sec-Ch-Ua-Platform", it) }
        }

    companion object {
        val FALLBACK =
            BrowserFingerprint(
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
                acceptLanguage = "en-US,en;q=0.9",
                secChUa = "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
                secChUaMobile = "?0",
                secChUaPlatform = "\"Windows\"",
            )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ScrapeOpsResponse(
    val result: List<BrowserFingerprint> = emptyList(),
)
