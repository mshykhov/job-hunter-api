package com.mshykhov.jobhunter.application.ai.dto

// Field order mirrors the response schema: reasoning is generated before the
// score so the model commits to its analysis first (chain-of-thought ordering).
data class JobRelevanceResult(val reasoning: String, val score: Int, val inferredRemote: Boolean)
