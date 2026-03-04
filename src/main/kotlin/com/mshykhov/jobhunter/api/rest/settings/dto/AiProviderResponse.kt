package com.mshykhov.jobhunter.api.rest.settings.dto

import com.mshykhov.jobhunter.application.settings.AiModel

data class AiProvidersResponse(
    val providers: List<AiProviderResponse>,
)

data class AiProviderResponse(
    val id: String,
    val name: String,
    val recommended: Boolean,
    val models: List<AiModelResponse>,
)

data class AiModelResponse(
    val id: String,
    val name: String,
    val inputCostPer1M: Double,
    val outputCostPer1M: Double,
    val cachedInputCostPer1M: Double?,
    val contextWindow: Int,
    val recommended: Boolean,
) {
    companion object {
        fun from(model: AiModel): AiModelResponse =
            AiModelResponse(
                id = model.id,
                name = model.displayName,
                inputCostPer1M = model.inputCostPer1M,
                outputCostPer1M = model.outputCostPer1M,
                cachedInputCostPer1M = model.cachedInputCostPer1M,
                contextWindow = model.contextWindow,
                recommended = model.recommended,
            )
    }
}
