package com.mshykhov.jobhunter.infrastructure.metrics

import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.settings.AiProvider
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.function.Supplier

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
        meterRegistry
            .counter(AI_EVALUATIONS_METRIC, PROVIDER_TAG, provider.name, MODEL_TAG, model, OUTCOME_TAG, outcome)
            .increment()
        meterRegistry
            .timer(AI_EVALUATION_DURATION_METRIC, PROVIDER_TAG, provider.name, MODEL_TAG, model)
            .record(duration)
    }

    fun recordIngest(
        source: JobSource,
        count: Int,
    ) {
        meterRegistry
            .counter(JOBS_INGESTED_METRIC, SOURCE_TAG, source.value)
            .increment(count.toDouble())
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
    }
}
