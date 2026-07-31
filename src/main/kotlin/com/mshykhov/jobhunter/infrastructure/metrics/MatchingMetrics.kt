package com.mshykhov.jobhunter.infrastructure.metrics

import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.settings.AiModel
import com.mshykhov.jobhunter.application.settings.AiProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.function.Supplier

private val logger = KotlinLogging.logger {}

@Component
class MatchingMetrics(private val meterRegistry: MeterRegistry, private val backlogSupplier: Supplier<Long>) {
    init {
        Gauge.builder(MATCHING_BACKLOG_METRIC, backlogSupplier).register(meterRegistry)
    }

    fun recordEvaluation(
        provider: AiProvider,
        model: String,
        outcome: String,
        duration: Duration,
    ) {
        try {
            val modelTag = if (model in KNOWN_MODEL_IDS) model else OTHER_MODEL_TAG
            meterRegistry
                .counter(AI_EVALUATIONS_METRIC, PROVIDER_TAG, provider.name, MODEL_TAG, modelTag, OUTCOME_TAG, outcome)
                .increment()
            meterRegistry
                .timer(AI_EVALUATION_DURATION_METRIC, PROVIDER_TAG, provider.name, MODEL_TAG, modelTag)
                .record(duration)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to record evaluation metric for provider $provider" }
        }
    }

    fun recordIngest(
        source: JobSource,
        count: Int,
    ) {
        try {
            meterRegistry
                .counter(JOBS_INGESTED_METRIC, SOURCE_TAG, source.value)
                .increment(count.toDouble())
        } catch (e: Exception) {
            logger.warn(e) { "Failed to record ingest metric for source $source" }
        }
    }

    companion object {
        const val AI_EVALUATIONS_METRIC = "jobhunter.ai.evaluations"
        const val AI_EVALUATION_DURATION_METRIC = "jobhunter.ai.evaluation.duration"
        const val MATCHING_BACKLOG_METRIC = "jobhunter.matching.backlog"
        const val JOBS_INGESTED_METRIC = "jobhunter.jobs.ingested"

        private const val PROVIDER_TAG = "provider"
        private const val MODEL_TAG = "model"
        private const val OUTCOME_TAG = "outcome"
        private const val SOURCE_TAG = "source"
        private const val OTHER_MODEL_TAG = "other"

        private val KNOWN_MODEL_IDS = AiModel.entries.map { it.id }.toSet()
    }
}
