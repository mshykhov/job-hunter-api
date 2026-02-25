package com.mshykhov.jobhunter.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.time.Clock
import java.time.temporal.TemporalAccessor
import java.util.Optional

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider")
class JpaAuditingConfig(
    private val clock: Clock,
) {
    @Bean
    fun dateTimeProvider(): DateTimeProvider =
        DateTimeProvider {
            Optional.of(clock.instant() as TemporalAccessor)
        }
}
