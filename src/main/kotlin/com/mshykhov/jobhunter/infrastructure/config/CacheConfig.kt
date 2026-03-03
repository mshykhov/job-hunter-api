package com.mshykhov.jobhunter.infrastructure.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun cacheManager(): CacheManager {
        val manager = CaffeineCacheManager()
        manager.registerCustomCache(
            PUBLIC_JOBS_CACHE,
            Caffeine
                .newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .build(),
        )
        return manager
    }

    companion object {
        const val PUBLIC_JOBS_CACHE = "publicJobs"
    }
}
