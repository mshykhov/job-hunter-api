package com.mshykhov.jobhunter.application.ai

enum class AiUseCase(val temperature: Double, val reasoningEffort: String) {
    SCORING(0.2, "low"),
    OUTREACH(0.7, "medium"),
    EXTRACTION(0.1, "low"),
    OPTIMIZATION(0.3, "low"),
}
