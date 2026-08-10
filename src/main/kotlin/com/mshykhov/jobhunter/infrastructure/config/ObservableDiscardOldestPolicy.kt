package com.mshykhov.jobhunter.infrastructure.config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor

private val logger = KotlinLogging.logger {}

internal class ObservableDiscardOldestPolicy(private val meterRegistry: MeterRegistry) : RejectedExecutionHandler {
    private val delegate = ThreadPoolExecutor.DiscardOldestPolicy()

    override fun rejectedExecution(
        task: Runnable,
        executor: ThreadPoolExecutor,
    ) {
        meterRegistry.counter(AsyncConfig.DISCARDED_TASKS_METRIC).increment()
        logger.warn { "Retro cold filter executor queue is full; discarding the oldest queued task" }
        delegate.rejectedExecution(task, executor)
    }
}
