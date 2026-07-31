package com.mshykhov.jobhunter.bench

import com.mshykhov.jobhunter.application.settings.AiModel
import com.mshykhov.jobhunter.application.settings.AiProvider
import java.time.Instant
import kotlin.math.abs
import kotlin.math.ceil

const val RELEVANCE_THRESHOLD = 60

data class BenchAttempt(
    val provider: AiProvider,
    val model: String,
    val jobId: String,
    val latencyMs: Long,
    val score: Int?,
    val reasoningLength: Int?,
    val outcome: String,
    val error: String?,
)

data class ProviderSkip(val provider: AiProvider, val reason: String)

data class ModelSummary(
    val provider: AiProvider,
    val model: String,
    val attempts: Int,
    val successes: Int,
    val meanAbsoluteError: Double?,
    val falsePositives: Int,
    val falseNegatives: Int,
    val medianLatencyMs: Long?,
    val p95LatencyMs: Long?,
    val estimatedCostPer1000: Double?,
    val schemaOutcomes: Map<String, Int>,
)

private const val CHARS_PER_TOKEN = 4.0
private const val ESTIMATED_SYSTEM_PROMPT_CHARS = 2500
private const val ESTIMATED_OUTPUT_OVERHEAD_CHARS = 40

fun summarize(
    attempts: List<BenchAttempt>,
    fixture: BenchFixture,
    labels: Map<String, BenchLabel>,
): List<ModelSummary> =
    attempts
        .groupBy { it.provider to it.model }
        .map { (key, group) -> buildSummary(key.first, key.second, group, fixture, labels) }
        .sortedWith(compareBy({ it.provider }, { it.model }))

private fun buildSummary(
    provider: AiProvider,
    model: String,
    group: List<BenchAttempt>,
    fixture: BenchFixture,
    labels: Map<String, BenchLabel>,
): ModelSummary {
    val successes = group.filter { it.score != null }
    val absoluteErrors =
        successes.mapNotNull { attempt ->
            val score = attempt.score ?: return@mapNotNull null
            val labelScore = labels[attempt.jobId]?.score ?: return@mapNotNull null
            abs(score - labelScore)
        }
    val inversionPairs =
        successes.mapNotNull { attempt ->
            val score = attempt.score ?: return@mapNotNull null
            val labelRelevant = labels[attempt.jobId]?.relevant ?: return@mapNotNull null
            (score >= RELEVANCE_THRESHOLD) to labelRelevant
        }
    val falsePositives = inversionPairs.count { (modelRelevant, labelRelevant) -> modelRelevant && !labelRelevant }
    val falseNegatives = inversionPairs.count { (modelRelevant, labelRelevant) -> !modelRelevant && labelRelevant }
    val latenciesMs = group.map { it.latencyMs }.sorted()
    val aiModel = AiModel.entries.find { it.id == model && it.provider == provider }
    return ModelSummary(
        provider = provider,
        model = model,
        attempts = group.size,
        successes = successes.size,
        meanAbsoluteError = absoluteErrors.takeIf { it.isNotEmpty() }?.average(),
        falsePositives = falsePositives,
        falseNegatives = falseNegatives,
        medianLatencyMs = percentile(latenciesMs, 0.5),
        p95LatencyMs = percentile(latenciesMs, 0.95),
        estimatedCostPer1000 = aiModel?.let { estimateCostPer1000(it, fixture, successes) },
        schemaOutcomes = group.groupingBy { it.outcome }.eachCount(),
    )
}

private fun percentile(
    sortedValues: List<Long>,
    fraction: Double,
): Long? {
    if (sortedValues.isEmpty()) return null
    val index = (ceil(fraction * sortedValues.size).toInt().coerceIn(1, sortedValues.size)) - 1
    return sortedValues[index]
}

private fun estimateCostPer1000(
    aiModel: AiModel,
    fixture: BenchFixture,
    successes: List<BenchAttempt>,
): Double? {
    if (successes.isEmpty()) return null
    val jobsById = fixture.jobs.associateBy { it.id }
    val inputTokenSamples =
        successes.mapNotNull { attempt ->
            val job = jobsById[attempt.jobId] ?: return@mapNotNull null
            (ESTIMATED_SYSTEM_PROMPT_CHARS + job.description.length + fixture.preference.about.orEmpty().length) / CHARS_PER_TOKEN
        }
    val outputTokenSamples =
        successes.mapNotNull { attempt -> attempt.reasoningLength?.let { (it + ESTIMATED_OUTPUT_OVERHEAD_CHARS) / CHARS_PER_TOKEN } }
    if (inputTokenSamples.isEmpty() || outputTokenSamples.isEmpty()) return null
    val avgInputTokens = inputTokenSamples.average()
    val avgOutputTokens = outputTokenSamples.average()
    val costPerCall = avgInputTokens / 1_000_000 * aiModel.inputCostPer1M + avgOutputTokens / 1_000_000 * aiModel.outputCostPer1M
    return costPerCall * 1000
}

fun renderMarkdown(
    fixture: BenchFixture,
    labels: Map<String, BenchLabel>,
    attempts: List<BenchAttempt>,
    skips: List<ProviderSkip>,
    generatedAt: Instant,
): String {
    val summaries = summarize(attempts, fixture, labels)
    return buildString {
        appendLine("# Provider Benchmark Report")
        appendLine()
        appendLine("Generated: $generatedAt")
        appendLine("Fixture jobs: ${fixture.jobs.size}, labeled: ${labels.size}")
        appendLine("A model score >= $RELEVANCE_THRESHOLD is treated as \"relevant\" for the false positive/negative counts.")
        appendLine()
        appendLine("This is one person's judgement on ${fixture.jobs.size} jobs. Treat it as a smoke test, not a leaderboard.")
        appendLine()
        appendLine("## Providers")
        appendLine()
        appendProviderTable(summaries)
        if (skips.isNotEmpty()) {
            appendLine()
            appendLine("## Skipped Providers")
            appendLine()
            skips.forEach { appendLine("- ${it.provider}: ${it.reason}") }
        }
        appendLine()
        appendLine("## Per-Job Detail")
        appendLine()
        appendPerJobTable(fixture, labels, attempts, summaries)
    }
}

private val PROVIDER_TABLE_COLUMNS =
    listOf(
        "Provider", "Model", "N", "Successes", "MAE", "False Positives", "False Negatives",
        "Median Latency", "P95 Latency", "Est. Cost / 1000 calls", "Schema Outcomes",
    )

private fun StringBuilder.appendProviderTable(summaries: List<ModelSummary>) {
    appendLine("| ${PROVIDER_TABLE_COLUMNS.joinToString(" | ")} |")
    appendLine("|${PROVIDER_TABLE_COLUMNS.joinToString("") { "---|" }}")
    summaries.forEach { s ->
        appendLine(
            "| ${s.provider} | ${s.model} | ${s.attempts} | ${s.successes} | ${formatDouble(s.meanAbsoluteError)} | " +
                "${s.falsePositives} | ${s.falseNegatives} | ${formatMs(s.medianLatencyMs)} | ${formatMs(s.p95LatencyMs)} | " +
                "${formatCost(s.estimatedCostPer1000)} | ${formatOutcomes(s.schemaOutcomes)} |",
        )
    }
}

private fun StringBuilder.appendPerJobTable(
    fixture: BenchFixture,
    labels: Map<String, BenchLabel>,
    attempts: List<BenchAttempt>,
    summaries: List<ModelSummary>,
) {
    val modelKeys = summaries.map { it.provider to it.model }
    val header = listOf("Job", "Label (relevant/score)") + modelKeys.map { "${it.first} ${it.second}" }
    appendLine("| ${header.joinToString(" | ")} |")
    appendLine("|${header.joinToString("") { "---|" }}")
    fixture.jobs.forEach { job ->
        val label = labels[job.id]
        val labelText = label?.let { "${it.relevant}/${it.score}" } ?: "unlabeled"
        val cells =
            modelKeys.map { (provider, model) ->
                val attempt = attempts.find { it.provider == provider && it.model == model && it.jobId == job.id }
                when {
                    attempt == null -> "-"
                    attempt.score != null -> attempt.score.toString()
                    else -> "FAIL(${attempt.outcome})"
                }
            }
        val row = listOf("${job.id} (${job.title})", labelText) + cells
        appendLine("| ${row.joinToString(" | ")} |")
    }
}

private fun formatDouble(value: Double?): String = value?.let { "%.1f".format(it) } ?: "-"

private fun formatMs(value: Long?): String = value?.let { "${it}ms" } ?: "-"

private fun formatCost(value: Double?): String = value?.let { "$%.4f".format(it) } ?: "-"

private fun formatOutcomes(outcomes: Map<String, Int>): String = outcomes.entries.joinToString(", ") { "${it.key}=${it.value}" }
