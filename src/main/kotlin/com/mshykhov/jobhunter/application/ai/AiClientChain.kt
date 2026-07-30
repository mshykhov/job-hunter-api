package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.application.settings.AiProvider
import org.springframework.ai.chat.client.ChatClient

data class AiClientLink(val provider: AiProvider, val modelId: String, val client: ChatClient)

data class AiClientChain(val links: List<AiClientLink>)
