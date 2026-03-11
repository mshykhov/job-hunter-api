package com.mshykhov.jobhunter.application.common

class AiNotConfiguredException(message: String = "AI is not configured. Please set your API key and model in settings.") : RuntimeException(message)
