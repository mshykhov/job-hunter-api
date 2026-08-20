package com.mshykhov.jobhunter.application.materials

enum class MaterialStatus {
    QUEUED,
    CLAIMED,
    GENERATING,
    VALIDATING,
    RENDERING,
    READY,
    READY_WITH_FALLBACK,
    BLOCKED,
    FAILED,
}
