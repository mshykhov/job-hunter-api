package com.mshykhov.jobhunter.application.ai.dto

data class JobRelevanceResult(
    val score: Int,
    val reasoning: String,
)
