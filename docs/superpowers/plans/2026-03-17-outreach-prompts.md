# Outreach Prompts & ChatClientFactory Improvement — Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix GPT-5 Nano compatibility and improve cover letter / recruiter message prompt quality.

**Architecture:** Add `AiUseCase` enum for per-task AI configuration, refactor `ChatClientFactory` to support reasoning models (reasoning_effort) vs standard models (temperature), replace outreach prompts with structured anti-cliche versions.

**Tech Stack:** Kotlin 2.1, Spring AI 1.0.0 (OpenAiChatOptions), MockK, JUnit 5

---

## Chunk 1: AiUseCase enum + ChatClientFactory refactor

### Task 1: Create AiUseCase enum

**Files:**
- Create: `src/main/kotlin/com/mshykhov/jobhunter/application/ai/AiUseCase.kt`

- [ ] **Step 1: Create AiUseCase enum**

```kotlin
package com.mshykhov.jobhunter.application.ai

enum class AiUseCase(
    val temperature: Double,
    val reasoningEffort: String,
) {
    SCORING(0.2, "low"),
    OUTREACH(0.7, "medium"),
    EXTRACTION(0.1, "low"),
    OPTIMIZATION(0.3, "low"),
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

### Task 2: Refactor ChatClientFactory

**Files:**
- Modify: `src/main/kotlin/com/mshykhov/jobhunter/application/ai/ChatClientFactory.kt`

- [ ] **Step 1: Rewrite ChatClientFactory**

Replace entire file content with:

```kotlin
package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.application.common.AiNotConfiguredException
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.stereotype.Component

@Component
class ChatClientFactory {
    fun createForUser(
        settings: UserAiSettingsEntity,
        useCase: AiUseCase = AiUseCase.SCORING,
    ): ChatClient {
        if (settings.apiKey.isBlank()) {
            throw AiNotConfiguredException("API key is corrupted or missing — please re-enter your API key in settings.")
        }
        val api = OpenAiApi.builder().apiKey(settings.apiKey).build()
        val options = buildOptions(settings.modelId, useCase)
        val model =
            OpenAiChatModel
                .builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build()
        return ChatClient.builder(model).build()
    }

    private fun buildOptions(
        modelId: String,
        useCase: AiUseCase,
    ): OpenAiChatOptions {
        val builder = OpenAiChatOptions.builder().model(modelId)
        if (isReasoningModel(modelId)) {
            builder.reasoningEffort(useCase.reasoningEffort)
        } else {
            builder.temperature(useCase.temperature)
        }
        return builder.build()
    }

    companion object {
        // OpenAI reasoning models that use reasoning_effort instead of temperature.
        // Non-OpenAI models (Claude, etc.) go through OpenAI-compatible API and receive temperature.
        private fun isReasoningModel(modelId: String): Boolean =
            modelId.startsWith("gpt-5") ||
                modelId.startsWith("o1") ||
                modelId.startsWith("o3") ||
                modelId.startsWith("o4")
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL (callers still compile because of default parameter `AiUseCase.SCORING`)

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/mshykhov/jobhunter/application/ai/AiUseCase.kt \
        src/main/kotlin/com/mshykhov/jobhunter/application/ai/ChatClientFactory.kt
git commit -m "feat: add AiUseCase enum and refactor ChatClientFactory for reasoning models"
```

### Task 3: Create ChatClientFactory unit test

**Files:**
- Create: `src/test/kotlin/com/mshykhov/jobhunter/application/ai/ChatClientFactoryTest.kt`

- [ ] **Step 1: Write tests for reasoning model detection and option building**

```kotlin
package com.mshykhov.jobhunter.application.ai

import com.mshykhov.jobhunter.application.common.AiNotConfiguredException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertNotNull

class ChatClientFactoryTest {
    private val factory = ChatClientFactory()

    @Nested
    inner class CreateForUser {
        @Test
        fun `should throw AiNotConfiguredException when API key is blank`() {
            val settings = mockk<UserAiSettingsEntity>()
            every { settings.apiKey } returns ""

            assertThrows<AiNotConfiguredException> {
                factory.createForUser(settings)
            }
        }

        @Test
        fun `should create client with default SCORING use case`() {
            val settings = mockk<UserAiSettingsEntity>()
            every { settings.apiKey } returns "test-key"
            every { settings.modelId } returns "gpt-4o-mini"

            val client = factory.createForUser(settings)

            assertNotNull(client)
        }

        @ParameterizedTest
        @ValueSource(strings = ["gpt-5-nano", "gpt-5", "o1-mini", "o3-mini", "o4-mini"])
        fun `should create client for reasoning models without error`(modelId: String) {
            val settings = mockk<UserAiSettingsEntity>()
            every { settings.apiKey } returns "test-key"
            every { settings.modelId } returns modelId

            val client = factory.createForUser(settings, AiUseCase.OUTREACH)

            assertNotNull(client)
        }

        @ParameterizedTest
        @ValueSource(strings = ["gpt-4o-mini", "gpt-4o", "claude-haiku", "claude-sonnet"])
        fun `should create client for standard models without error`(modelId: String) {
            val settings = mockk<UserAiSettingsEntity>()
            every { settings.apiKey } returns "test-key"
            every { settings.modelId } returns modelId

            val client = factory.createForUser(settings, AiUseCase.SCORING)

            assertNotNull(client)
        }
    }
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew test --tests "com.mshykhov.jobhunter.application.ai.ChatClientFactoryTest"`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add src/test/kotlin/com/mshykhov/jobhunter/application/ai/ChatClientFactoryTest.kt
git commit -m "test: add ChatClientFactory unit tests for reasoning model detection"
```

## Chunk 2: Update callers to pass AiUseCase

### Task 4: Update OutreachService callers

**Files:**
- Modify: `src/main/kotlin/com/mshykhov/jobhunter/application/outreach/OutreachService.kt:137,154`

- [ ] **Step 1: Add AiUseCase import and update both call sites**

Add import at top:
```kotlin
import com.mshykhov.jobhunter.application.ai.AiUseCase
```

In `resolveTestContext()` (line 137), change:
```kotlin
val chatClient = chatClientFactory.createForUser(aiSettings)
```
to:
```kotlin
val chatClient = chatClientFactory.createForUser(aiSettings, AiUseCase.OUTREACH)
```

In `resolveGenerationContext()` (line 154), change:
```kotlin
val chatClient = chatClientFactory.createForUser(aiSettings)
```
to:
```kotlin
val chatClient = chatClientFactory.createForUser(aiSettings, AiUseCase.OUTREACH)
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

### Task 5: Update PreferenceService callers

**Files:**
- Modify: `src/main/kotlin/com/mshykhov/jobhunter/application/preference/PreferenceService.kt:82,100`

- [ ] **Step 1: Add AiUseCase import and update both call sites**

Add import at top:
```kotlin
import com.mshykhov.jobhunter.application.ai.AiUseCase
```

In `optimizeAbout()` (line 82), change:
```kotlin
val chatClient = chatClientFactory.createForUser(settings)
```
to:
```kotlin
val chatClient = chatClientFactory.createForUser(settings, AiUseCase.OPTIMIZATION)
```

In `generatePreferences()` (line 100), change:
```kotlin
val chatClient = chatClientFactory.createForUser(settings)
```
to:
```kotlin
val chatClient = chatClientFactory.createForUser(settings, AiUseCase.EXTRACTION)
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

### Task 6: Update JobMatchingService caller (explicit use case)

**Files:**
- Modify: `src/main/kotlin/com/mshykhov/jobhunter/application/matching/JobMatchingService.kt:218`

- [ ] **Step 1: Add import and update call site**

Add import at top:
```kotlin
import com.mshykhov.jobhunter.application.ai.AiUseCase
```

In `buildUserChatClients()` (line 218), change:
```kotlin
userId to chatClientFactory.createForUser(settings)
```
to:
```kotlin
userId to chatClientFactory.createForUser(settings, AiUseCase.SCORING)
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit all caller updates**

```bash
git add src/main/kotlin/com/mshykhov/jobhunter/application/outreach/OutreachService.kt \
        src/main/kotlin/com/mshykhov/jobhunter/application/preference/PreferenceService.kt \
        src/main/kotlin/com/mshykhov/jobhunter/application/matching/JobMatchingService.kt
git commit -m "feat: pass AiUseCase to ChatClientFactory in all callers"
```

## Chunk 3: Update outreach prompts

### Task 7: Replace default prompts in OutreachGenerator

**Files:**
- Modify: `src/main/kotlin/com/mshykhov/jobhunter/application/ai/OutreachGenerator.kt:86-100`

- [ ] **Step 1: Replace DEFAULT_COVER_LETTER_PROMPT (lines 86-92)**

Replace:
```kotlin
        val DEFAULT_COVER_LETTER_PROMPT =
            """
            Write a 2-3 sentence cover letter for this job application.
            Be concise, professional, and specific to the role.
            Mention relevant skills from the candidate's background.
            Output plain text only, no greeting or sign-off.
            """.trimIndent()
```

With:
```kotlin
        val DEFAULT_COVER_LETTER_PROMPT =
            """
            You write cover letter text for job applications.

            <rules>
            - Write 3-4 sentences, one paragraph
            - First person, natural professional tone
            - Sentence 1: name the specific role and connect your strongest matching skill to their key requirement
            - Sentence 2-3: briefly explain why you fit — reference specific technologies or experience from both the job and your background
            - Last sentence: express interest in contributing to their team/product
            </rules>

            <forbidden>
            - "I am writing to express my interest"
            - "I am passionate about" / "I am excited to"
            - "I believe I would be a great fit"
            - "With my X years of experience"
            - "I am confident that"
            - Any greeting (Dear, Hello) or sign-off (Sincerely, Best regards)
            </forbidden>

            Output ONLY the cover letter text. Plain text, no markdown.
            """.trimIndent()
```

- [ ] **Step 2: Replace DEFAULT_RECRUITER_MESSAGE_PROMPT (lines 94-100)**

Replace:
```kotlin
        val DEFAULT_RECRUITER_MESSAGE_PROMPT =
            """
            Write a 2-3 sentence message to a recruiter about this job.
            Tone: friendly, professional, brief.
            Mention you've applied and express genuine interest.
            Output plain text only.
            """.trimIndent()
```

With:
```kotlin
        val DEFAULT_RECRUITER_MESSAGE_PROMPT =
            """
            You write short messages from a candidate to a recruiter about a specific role.

            <rules>
            - Write 2-3 sentences
            - Conversational, confident, peer-to-peer tone
            - Sentence 1: mention the role and one specific thing that caught your attention (tech stack, product, company mission)
            - Sentence 2: your relevant experience — name specific technologies, not generic claims
            - Last sentence: suggest a brief chat or ask if the role is still open
            </rules>

            <forbidden>
            - "I am reaching out regarding" / "I came across your posting"
            - "I am passionate about" / "I am excited about"
            - "I believe my skills align perfectly"
            - "I would love the opportunity"
            - Any greeting or sign-off
            - Exclamation marks
            </forbidden>

            Output ONLY the message text. Plain text, no markdown.
            """.trimIndent()
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/com/mshykhov/jobhunter/application/ai/OutreachGenerator.kt
git commit -m "feat: improve outreach prompts with anti-cliche rules and structured format"
```

## Chunk 4: Update tests

### Task 8: Update OutreachServiceTest mocks

**Files:**
- Modify: `src/test/kotlin/com/mshykhov/jobhunter/application/outreach/OutreachServiceTest.kt`

- [ ] **Step 1: Add AiUseCase import**

Add at top of file:
```kotlin
import com.mshykhov.jobhunter.application.ai.AiUseCase
```

- [ ] **Step 2: Update all `chatClientFactory.createForUser` mocks**

Replace all occurrences (lines 63, 96, 163, 231, 269):
```kotlin
every { chatClientFactory.createForUser(aiSettings) } returns chatClient
```
with:
```kotlin
every { chatClientFactory.createForUser(aiSettings, AiUseCase.OUTREACH) } returns chatClient
```

There are 5 occurrences total in the file.

- [ ] **Step 3: Run OutreachServiceTest**

Run: `./gradlew test --tests "com.mshykhov.jobhunter.application.outreach.OutreachServiceTest"`
Expected: All tests PASS

### Task 9: Update JobMatchingServiceTest mocks

**Files:**
- Modify: `src/test/kotlin/com/mshykhov/jobhunter/application/matching/JobMatchingServiceTest.kt`

- [ ] **Step 1: Add AiUseCase import**

Add at top of file:
```kotlin
import com.mshykhov.jobhunter.application.ai.AiUseCase
```

- [ ] **Step 2: Update all `chatClientFactory.createForUser` mocks**

Replace all occurrences (lines 116, 143, 199, 306):
```kotlin
every { chatClientFactory.createForUser(aiSettings) } returns chatClient
```
with:
```kotlin
every { chatClientFactory.createForUser(aiSettings, AiUseCase.SCORING) } returns chatClient
```

There are 4 occurrences total in the file.

- [ ] **Step 3: Run JobMatchingServiceTest**

Run: `./gradlew test --tests "com.mshykhov.jobhunter.application.matching.JobMatchingServiceTest"`
Expected: All tests PASS

### Task 10: Update PreferenceServiceTest mock

**Files:**
- Modify: `src/test/kotlin/com/mshykhov/jobhunter/application/preference/PreferenceServiceTest.kt`

- [ ] **Step 1: Add AiUseCase import**

Add at top of file:
```kotlin
import com.mshykhov.jobhunter.application.ai.AiUseCase
```

- [ ] **Step 2: Update `chatClientFactory.createForUser` mock**

Replace (line 54):
```kotlin
every { chatClientFactory.createForUser(aiSettings) } returns chatClient
```
with:
```kotlin
every { chatClientFactory.createForUser(aiSettings, AiUseCase.OPTIMIZATION) } returns chatClient
```

- [ ] **Step 3: Run PreferenceServiceTest**

Run: `./gradlew test --tests "com.mshykhov.jobhunter.application.preference.PreferenceServiceTest"`
Expected: All tests PASS

- [ ] **Step 4: Run full test suite**

Run: `./gradlew test`
Expected: All tests PASS

- [ ] **Step 5: Run ktlint**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL (no violations)

- [ ] **Step 6: Commit all test updates**

```bash
git add src/test/kotlin/com/mshykhov/jobhunter/application/outreach/OutreachServiceTest.kt \
        src/test/kotlin/com/mshykhov/jobhunter/application/matching/JobMatchingServiceTest.kt \
        src/test/kotlin/com/mshykhov/jobhunter/application/preference/PreferenceServiceTest.kt
git commit -m "test: update mocks to pass AiUseCase parameter to ChatClientFactory"
```
