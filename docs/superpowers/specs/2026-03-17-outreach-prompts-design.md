# Outreach Prompts & ChatClientFactory Improvement — Design Spec

> **Date**: 2026-03-17
> **Status**: Draft
> **Goal**: Fix GPT-5 Nano compatibility, improve cover letter and recruiter message prompt quality.

---

## Problem Statement

1. **ChatClientFactory is broken for GPT-5 Nano** — sets `temperature=1.0`, but GPT-5 Nano does not accept the `temperature` parameter (API error). Reasoning models use `reasoning_effort` instead.
2. **Outreach prompts produce generic AI-sounding text** — "I am passionate about...", "I am writing to express my interest..." cliches.
3. **Cover letters don't connect candidate skills to job requirements** — too much generic filler, not enough specificity.
4. **Same temperature for all use cases** — scoring (needs determinism) and outreach (needs creativity) use the same config.

---

## Design

### 1. New Cover Letter Prompt

Replaces `OutreachGenerator.DEFAULT_COVER_LETTER_PROMPT`.

```
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
```

### 2. New Recruiter Message Prompt

Replaces `OutreachGenerator.DEFAULT_RECRUITER_MESSAGE_PROMPT`.

```
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
```

### 3. AiUseCase Enum

New enum in `application/ai/AiUseCase.kt`:

```kotlin
enum class AiUseCase(val temperature: Double, val reasoningEffort: String) {
    SCORING(0.2, "low"),
    OUTREACH(0.7, "medium"),
    EXTRACTION(0.1, "minimal"),
    OPTIMIZATION(0.3, "low"),
}
```

| Use Case | temperature (non-reasoning) | reasoning_effort (GPT-5/o-series) | Rationale |
|----------|---------------------------|----------------------------------|-----------|
| SCORING | 0.2 | low | Deterministic analysis; minimal is risky (Nano hallucinations documented) |
| OUTREACH | 0.7 | medium | Creative text with forbidden-phrase rules; needs enough reasoning to follow constraints |
| EXTRACTION | 0.1 | minimal | Pure extraction (text to JSON); OpenAI recommends minimal for extraction tasks |
| OPTIMIZATION | 0.3 | low | Semi-creative restructuring; more than extraction, less than creative writing |

### 4. ChatClientFactory Changes

**File**: `application/ai/ChatClientFactory.kt`

Current signature:
```kotlin
fun createForUser(settings: UserAiSettingsEntity): ChatClient
```

New signature:
```kotlin
fun createForUser(settings: UserAiSettingsEntity, useCase: AiUseCase = AiUseCase.SCORING): ChatClient
```

Logic:
```kotlin
fun isReasoningModel(modelId: String): Boolean =
    modelId.startsWith("gpt-5") ||
    modelId.startsWith("o1") ||
    modelId.startsWith("o3") ||
    modelId.startsWith("o4")

// In createForUser:
if (isReasoningModel(settings.modelId)) {
    // Set reasoning_effort, NO temperature
    options.reasoningEffort(useCase.reasoningEffort)
} else {
    // Set temperature, NO reasoning_effort
    options.temperature(useCase.temperature)
}
```

### 5. Caller Updates

Each AI service passes its use case to `ChatClientFactory`:

| Caller | Use Case |
|--------|----------|
| `JobRelevanceEvaluator` | `AiUseCase.SCORING` |
| `OutreachGenerator` (via `OutreachService`) | `AiUseCase.OUTREACH` |
| `PreferenceNormalizer` | `AiUseCase.EXTRACTION` |
| `AboutOptimizer` | `AiUseCase.OPTIMIZATION` |

**Note**: Some callers create the ChatClient in the service layer and pass it to the AI class. The use case parameter needs to be threaded through wherever `createForUser()` is called.

### 6. User Prompt (no changes)

The existing `buildUserPrompt` in `OutreachGenerator` is fine:
- Job title, company, description (3000 chars)
- Candidate background (about text)

No changes needed here — the system prompt does the heavy lifting.

---

## Files Changed

| File | Change |
|------|--------|
| `application/ai/AiUseCase.kt` | **NEW** — enum with temperature + reasoning_effort per use case |
| `application/ai/ChatClientFactory.kt` | Add `useCase` parameter, reasoning model detection, conditional config |
| `application/ai/OutreachGenerator.kt` | Replace both DEFAULT prompts |
| All callers of `ChatClientFactory.createForUser()` | Pass appropriate `AiUseCase` |
| `OutreachServiceTest.kt` | Update mock assertions for new prompt text |
| `ChatClientFactoryTest.kt` | **NEW or update** — test reasoning model detection + use case config |

---

## Out of Scope

- Changing `JobRelevanceEvaluator` system prompt (separate task, covered in AI_MATCHING_ANALYSIS.md)
- Few-shot examples in prompts (decided against — approach A chosen)
- Two-phase extract-then-write pipeline (decided against — too complex for current needs)
- Model recommendation UI (future work)

---

## Expected Outcome

### Cover Letter — Before
```
I am writing to express my interest in the Senior Java Engineer position at AUTO1 Group.
With my experience in Java and Spring Boot, I believe I would be a great fit for this role.
I am excited about the opportunity to contribute to your team.
```

### Cover Letter — After
```
Building microservices with Java and Spring Boot is what I do daily, and AUTO1's Senior
Engineer role calls for exactly that stack. My recent work on BFF architecture and a
Spring 6 migration maps well to the modernization challenges described in the posting.
I'd be glad to bring that hands-on backend experience to AUTO1's vehicle marketplace platform.
```

### Recruiter Message — Before
```
I wanted to reach out about the Senior Java Engineer position at AUTO1 Group.
I have applied and am genuinely interested in this opportunity.
I believe my Java and Spring Boot experience makes me a strong candidate.
```

### Recruiter Message — After
```
The Senior Java Engineer role at AUTO1 caught my eye — building vehicle marketplace
infrastructure with Spring Boot and PostgreSQL is close to what I do daily. I've been
working with Kotlin and Java backends for 5+ years, most recently leading API design
and cloud migrations. Would be great to chat if the role is still open.
```
