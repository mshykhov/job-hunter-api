package com.mshykhov.jobhunter.application.matching

internal data class MatchingStats(
    var coldRejected: Int = 0,
    var alreadyMatched: Int = 0,
    var aiEvaluated: Int = 0,
    var postAiRejected: Int = 0,
    var saved: Int = 0,
    var aiFailed: Int = 0,
    var coldOnly: Int = 0,
) {
    fun merge(other: MatchingStats): MatchingStats =
        copy(
            coldRejected = coldRejected + other.coldRejected,
            alreadyMatched = alreadyMatched + other.alreadyMatched,
            aiEvaluated = aiEvaluated + other.aiEvaluated,
            postAiRejected = postAiRejected + other.postAiRejected,
            saved = saved + other.saved,
            aiFailed = aiFailed + other.aiFailed,
            coldOnly = coldOnly + other.coldOnly,
        )

    fun summary(): String =
        "coldRejected=$coldRejected alreadyMatched=$alreadyMatched " +
            "aiEvaluated=$aiEvaluated postAiRejected=$postAiRejected " +
            "saved=$saved aiFailed=$aiFailed coldOnly=$coldOnly"
}
