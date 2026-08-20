package com.mshykhov.jobhunter.application.statistics

enum class DecisionOutcome {
    COLD_REJECTED,
    AI_REJECTED_REMOTE,
    AI_SCORED,
    COLD_ONLY,
    LEGACY_REJECTED_UNKNOWN,
}
