package com.mshykhov.jobhunter.application.settings

enum class AiProvider(
    val id: String,
    val displayName: String,
    val recommended: Boolean = false,
) {
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
    GPT_5_2_PRO("gpt-5.2-pro", "GPT-5.2 Pro", AiProvider.OPENAI, 21.0, 168.0, null, 400_000),
    GPT_5_2("gpt-5.2", "GPT-5.2", AiProvider.OPENAI, 1.75, 14.0, 0.175, 400_000),
    GPT_5_1("gpt-5.1", "GPT-5.1", AiProvider.OPENAI, 1.25, 10.0, 0.125, 400_000),
    GPT_5("gpt-5", "GPT-5", AiProvider.OPENAI, 1.25, 10.0, 0.125, 400_000),
    GPT_5_MINI("gpt-5-mini", "GPT-5 Mini", AiProvider.OPENAI, 0.25, 2.0, 0.025, 400_000),
    GPT_5_NANO("gpt-5-nano", "GPT-5 Nano", AiProvider.OPENAI, 0.05, 0.40, 0.005, 400_000, recommended = true),

    GPT_4_1("gpt-4.1", "GPT-4.1", AiProvider.OPENAI, 2.0, 8.0, 0.50, 1_047_576),
    GPT_4_1_MINI("gpt-4.1-mini", "GPT-4.1 Mini", AiProvider.OPENAI, 0.40, 1.60, 0.10, 1_047_576),
    GPT_4_1_NANO("gpt-4.1-nano", "GPT-4.1 Nano", AiProvider.OPENAI, 0.10, 0.40, 0.025, 1_047_576),

    O3_PRO("o3-pro", "o3-pro", AiProvider.OPENAI, 20.0, 80.0, null, 200_000),
    O3("o3", "o3", AiProvider.OPENAI, 2.0, 8.0, 0.50, 200_000),
    O4_MINI("o4-mini", "o4-mini", AiProvider.OPENAI, 1.10, 4.40, 0.275, 200_000),

    GPT_4O("gpt-4o", "GPT-4o", AiProvider.OPENAI, 2.50, 10.0, 1.25, 128_000),
    GPT_4O_MINI("gpt-4o-mini", "GPT-4o Mini", AiProvider.OPENAI, 0.15, 0.60, 0.075, 128_000),
}
