package com.mshykhov.jobhunter.infrastructure.config

import com.mshykhov.jobhunter.application.matching.COLD_FILTER_RETRO_EXECUTOR
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Bean
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AsyncConfigTest {
    @Test
    fun `should configure bounded single-worker retro filter executor`() {
        val executor = AsyncConfig(SimpleMeterRegistry()).coldFilterRetroExecutor()
        val beanNames =
            AsyncConfig::class.java
                .getDeclaredMethod("coldFilterRetroExecutor")
                .getAnnotation(Bean::class.java)
                .value
                .toList()

        try {
            assertEquals(1, executor.corePoolSize)
            assertEquals(1, executor.maxPoolSize)
            assertEquals(16, executor.threadPoolExecutor.queue.remainingCapacity())
            assertEquals("cold-filter-retro-", executor.threadNamePrefix)
            assertIs<ObservableDiscardOldestPolicy>(executor.threadPoolExecutor.rejectedExecutionHandler)
            assertEquals(listOf(COLD_FILTER_RETRO_EXECUTOR), beanNames)
        } finally {
            executor.shutdown()
        }
    }

    @Test
    fun `should record discarded maintenance work when executor queue is saturated`() {
        val meterRegistry = SimpleMeterRegistry()
        val executor = AsyncConfig(meterRegistry).coldFilterRetroExecutor()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)

        try {
            executor.execute {
                started.countDown()
                release.await()
            }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            repeat(16) { executor.execute {} }

            executor.execute {}

            assertEquals(1.0, meterRegistry.counter(AsyncConfig.DISCARDED_TASKS_METRIC).count())
        } finally {
            release.countDown()
            executor.shutdown()
        }
    }
}
