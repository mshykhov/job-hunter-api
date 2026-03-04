package com.mshykhov.jobhunter.application.matching

internal sealed class FilterResult {
    data object Passed : FilterResult()

    data class Rejected(
        val filter: String,
        val reason: String,
    ) : FilterResult()
}
