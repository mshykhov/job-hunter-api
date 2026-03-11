package com.mshykhov.jobhunter.application.settings

enum class AiProvider(val id: String, val displayName: String, val recommended: Boolean = false) {
    OPENAI("openai", "OpenAI", recommended = true),
}

@Suppress("EnumNaming")
enum class AiModel(
    val id: String,
    val displayName: String,
    val provider: AiProvider,
    val inputCostPer1M: Double,
    val outputCostPer1M: Double,
    val cachedInputCostPer1M: Double?,
    val contextWindow: Int,
    val recommended: Boolean = false,
) {
    GPT_5_MINI("gpt-5-mini", "GPT-5 Mini", AiProvider.OPENAI, 0.25, 2.0, 0.025, 400_000),
    GPT_5_NANO("gpt-5-nano", "GPT-5 Nano", AiProvider.OPENAI, 0.05, 0.40, 0.005, 400_000, recommended = true),
}
