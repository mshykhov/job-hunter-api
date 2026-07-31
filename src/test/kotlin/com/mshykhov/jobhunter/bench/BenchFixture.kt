package com.mshykhov.jobhunter.bench

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

data class BenchPreference(
    val categories: List<String> = emptyList(),
    val remoteOnly: Boolean = false,
    val locations: List<String> = emptyList(),
    val excludedKeywords: List<String> = emptyList(),
    val about: String? = null,
    val customPrompt: String? = null,
)

data class BenchJob(
    val id: String,
    val title: String,
    val company: String? = null,
    val location: String? = null,
    val salary: String? = null,
    val remote: Boolean? = null,
    val description: String,
)

data class BenchFixture(val preference: BenchPreference, val jobs: List<BenchJob>)

data class BenchLabel(val relevant: Boolean? = null, val score: Int? = null)

class MissingBenchLabelsException(message: String) : RuntimeException(message)

object BenchFixtureLoader {
    private const val LOCAL_FIXTURE = "/bench/fixture.local.json"
    private const val LOCAL_LABELS = "/bench/labels.local.json"
    private const val EXAMPLE_FIXTURE = "/bench/fixture.example.json"
    private const val EXAMPLE_LABELS = "/bench/labels.example.json"

    private val mapper = jacksonObjectMapper()

    fun load(): Pair<BenchFixture, Map<String, BenchLabel>> {
        val useLocal = resourceExists(LOCAL_FIXTURE)
        val fixturePath = if (useLocal) LOCAL_FIXTURE else EXAMPLE_FIXTURE
        val labelsPath = if (useLocal) LOCAL_LABELS else EXAMPLE_LABELS
        val source = if (useLocal) "local" else "example"

        val fixture: BenchFixture = mapper.readValue(readResource(fixturePath))
        if (!resourceExists(labelsPath)) {
            throw missingLabelsError(source, "$labelsPath is missing on the classpath.")
        }
        val labels: Map<String, BenchLabel> = mapper.readValue(readResource(labelsPath))

        val unlabeled = fixture.jobs.filter { job -> isIncomplete(labels[job.id]) }
        if (unlabeled.isNotEmpty()) {
            throw missingLabelsError(
                source,
                "these job ids have no relevant/score label yet: ${unlabeled.joinToString(", ") { it.id }}.",
            )
        }
        return fixture to labels
    }

    private fun isIncomplete(label: BenchLabel?): Boolean = label == null || label.relevant == null || label.score == null

    private fun resourceExists(path: String): Boolean = javaClass.getResource(path) != null

    private fun readResource(path: String): String =
        javaClass.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("Bench resource not found on classpath: $path")

    private fun missingLabelsError(source: String, detail: String): MissingBenchLabelsException =
        MissingBenchLabelsException(
            "Cannot run the benchmark against the $source fixture: $detail " +
                "A benchmark with no ground truth produces a table that looks authoritative and means nothing. " +
                "Run scripts/export-bench-fixture.sh to produce src/test/resources/bench/fixture.local.json and " +
                "labels.local.json, fill in relevant/score for every job, then run ./gradlew bench again.",
        )
}
