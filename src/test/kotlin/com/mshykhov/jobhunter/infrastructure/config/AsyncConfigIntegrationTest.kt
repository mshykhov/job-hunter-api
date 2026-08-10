package com.mshykhov.jobhunter.infrastructure.config

import com.mshykhov.jobhunter.application.matching.COLD_FILTER_RETRO_EXECUTOR
import com.mshykhov.jobhunter.support.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.Async
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Import(AsyncConfigIntegrationTest.ProbeConfig::class)
class AsyncConfigIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    lateinit var probeState: AsyncProbeState

    @Test
    fun `should handle transactional event asynchronously after commit in a new transaction`() {
        transactionTemplate.executeWithoutResult {
            eventPublisher.publishEvent(AsyncProbeEvent())

            assertFalse(probeState.completed.await(100, TimeUnit.MILLISECONDS))
        }

        assertTrue(probeState.completed.await(5, TimeUnit.SECONDS))
        assertTrue(probeState.threadName.startsWith("cold-filter-retro-"))
        assertTrue(probeState.transactionActive)
    }

    @TestConfiguration(proxyBeanMethods = false)
    class ProbeConfig {
        @Bean
        fun asyncProbeState() = AsyncProbeState()

        @Bean
        fun asyncTransactionProbe(state: AsyncProbeState) = AsyncTransactionProbe(state)
    }

    class AsyncProbeState {
        val completed = CountDownLatch(1)

        @Volatile
        var threadName = ""

        @Volatile
        var transactionActive = false
    }

    open class AsyncTransactionProbe(private val state: AsyncProbeState) {

        @Async(COLD_FILTER_RETRO_EXECUTOR)
        @TransactionalEventListener
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        open fun handle(event: AsyncProbeEvent) {
            state.threadName = Thread.currentThread().name
            state.transactionActive = TransactionSynchronizationManager.isActualTransactionActive()
            state.completed.countDown()
        }
    }

    class AsyncProbeEvent
}
