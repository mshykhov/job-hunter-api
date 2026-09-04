package com.mshykhov.jobhunter.application.automation.workflow

enum class AutomationAttemptOutcome {
    ACTIVE,
    SUCCEEDED,
    PAUSED,
    STOPPED,
    STALE_GENERATION,
    LEASE_EXPIRED,
    FAILED,
}
