package com.mshykhov.jobhunter.application.common

class StaleAutomationGenerationException : RuntimeException("Stale runner generation")

class AutomationLeaseLostException : RuntimeException("Automation work lease is stale or invalid")
