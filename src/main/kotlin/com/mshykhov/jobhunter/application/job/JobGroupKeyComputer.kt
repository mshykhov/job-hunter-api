package com.mshykhov.jobhunter.application.job

import java.security.MessageDigest

object JobGroupKeyComputer {
    private val WHITESPACE_REGEX = Regex("\\s+")

    fun compute(
        title: String,
        company: String?,
    ): String {
        val normalizedTitle = title.lowercase().trim().replace(WHITESPACE_REGEX, " ")
        val normalizedCompany =
            company
                ?.lowercase()
                ?.trim()
                ?.replace(WHITESPACE_REGEX, " ")
                .orEmpty()
        val normalized = "$normalizedTitle::$normalizedCompany"
        val digest = MessageDigest.getInstance("MD5").digest(normalized.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
