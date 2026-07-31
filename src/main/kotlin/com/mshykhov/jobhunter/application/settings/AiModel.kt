package com.mshykhov.jobhunter.application.settings

import com.fasterxml.jackson.annotation.JsonCreator
import com.mshykhov.jobhunter.application.common.ValueMappedEnum

enum class AiProvider(override val value: String, val displayName: String, val requiresApiKey: Boolean = true, val recommended: Boolean = false) :
    ValueMappedEnum {
    CODEX("codex", "Codex subscription", requiresApiKey = false, recommended = true),
    OPENAI("openai", "OpenAI"),
    GEMINI("gemini", "Google Gemini"),
    ;

    override fun toString(): String = value

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): AiProvider = entries.first { it.value.equals(value, ignoreCase = true) }
    }
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
    GPT_5_6_LUNA("gpt-5.6-luna", "GPT-5.6 Luna", AiProvider.CODEX, 0.0, 0.0, null, 400_000, recommended = true),
    GPT_5_6_SOL("gpt-5.6-sol", "GPT-5.6 Sol", AiProvider.CODEX, 0.0, 0.0, null, 400_000),
    GPT_5_MINI("gpt-5-mini", "GPT-5 Mini", AiProvider.OPENAI, 0.25, 2.0, 0.025, 400_000),
    GPT_5_NANO("gpt-5-nano", "GPT-5 Nano", AiProvider.OPENAI, 0.05, 0.40, 0.005, 400_000),
    GEMINI_2_5_FLASH_LITE("gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite", AiProvider.GEMINI, 0.10, 0.40, null, 1_000_000),
}
