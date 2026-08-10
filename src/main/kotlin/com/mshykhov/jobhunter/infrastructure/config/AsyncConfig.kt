package com.mshykhov.jobhunter.infrastructure.config

import com.mshykhov.jobhunter.application.matching.COLD_FILTER_RETRO_EXECUTOR
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@EnableAsync
class AsyncConfig(private val meterRegistry: MeterRegistry) {
    @Bean(COLD_FILTER_RETRO_EXECUTOR)
    fun coldFilterRetroExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 1
            maxPoolSize = 1
            queueCapacity = 16
            setThreadNamePrefix("cold-filter-retro-")
            setRejectedExecutionHandler(ObservableDiscardOldestPolicy(meterRegistry))
            initialize()
        }

    companion object {
        const val DISCARDED_TASKS_METRIC = "jobhunter.matching.retro.discarded"
    }
}
