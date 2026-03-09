package com.mshykhov.jobhunter.support

import com.mshykhov.jobhunter.api.rest.job.dto.JobIngestRequest
import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.preference.MatchingPreferences
import com.mshykhov.jobhunter.application.preference.SearchPreferences
import com.mshykhov.jobhunter.application.preference.TelegramPreferences
import com.mshykhov.jobhunter.application.preference.UserPreferenceEntity
import com.mshykhov.jobhunter.application.user.UserEntity
import com.mshykhov.jobhunter.application.userjob.UserJobEntity
import com.mshykhov.jobhunter.application.userjob.UserJobStatus
import java.util.UUID

object TestFixtures {
    fun jobIngestRequest(
        title: String = "Senior Kotlin Developer",
        company: String? = "TechCorp",
        url: String = "https://example.com/jobs/${UUID.randomUUID()}",
        description: String = "Looking for a senior Kotlin developer with Spring Boot experience",
        source: JobSource = JobSource.DOU,
        salary: String? = "5000 USD",
        location: String? = "Remote",
        remote: Boolean? = true,
        publishedAt: String? = "2026-03-01T10:00:00Z",
        rawData: Map<String, Any?> = emptyMap(),
    ): JobIngestRequest =
        JobIngestRequest(
            title = title,
            company = company,
            url = url,
            description = description,
            source = source,
            salary = salary,
            location = location,
            remote = remote,
            publishedAt = publishedAt,
            rawData = rawData,
        )

    fun jobEntity(
        title: String = "Senior Kotlin Developer",
        company: String? = "TechCorp",
        url: String = "https://example.com/jobs/${UUID.randomUUID()}",
        description: String = "Looking for a senior Kotlin developer with Spring Boot experience",
        source: JobSource = JobSource.DOU,
        remote: Boolean? = true,
        salary: String? = "5000 USD",
        location: String? = "Remote",
    ): JobEntity =
        JobEntity(
            title = title,
            company = company,
            url = url,
            description = description,
            source = source,
            remote = remote,
            salary = salary,
            location = location,
        )

    fun userEntity(auth0Sub: String = "auth0|test-user-${UUID.randomUUID()}"): UserEntity = UserEntity(auth0Sub = auth0Sub)

    fun userPreferenceEntity(
        user: UserEntity = userEntity(),
        about: String? = "Experienced Kotlin developer",
        remoteOnly: Boolean = false,
        disabledSources: List<JobSource> = emptyList(),
        categories: List<String> = emptyList(),
        locations: List<String> = emptyList(),
        keywords: List<String> = listOf("Kotlin", "Spring"),
        excludedKeywords: List<String> = emptyList(),
        excludedTitleKeywords: List<String> = emptyList(),
        excludedCompanies: List<String> = emptyList(),
        matchWithAi: Boolean = false,
    ): UserPreferenceEntity =
        UserPreferenceEntity(
            user = user,
            about = about,
            search =
                SearchPreferences(
                    remoteOnly = remoteOnly,
                    disabledSources = disabledSources,
                    categories = categories,
                    locations = locations,
                ),
            matching =
                MatchingPreferences(
                    keywords = keywords,
                    excludedKeywords = excludedKeywords,
                    excludedTitleKeywords = excludedTitleKeywords,
                    excludedCompanies = excludedCompanies,
                    matchWithAi = matchWithAi,
                ),
            telegram = TelegramPreferences(),
        )

    fun userJobEntity(
        user: UserEntity = userEntity(),
        job: JobEntity = jobEntity(),
        status: UserJobStatus = UserJobStatus.NEW,
        aiRelevanceScore: Int = 75,
        aiReasoning: String = "Good match for Kotlin developer",
        aiInferredRemote: Boolean? = true,
    ): UserJobEntity =
        UserJobEntity(
            user = user,
            job = job,
            status = status,
            aiRelevanceScore = aiRelevanceScore,
            aiReasoning = aiReasoning,
            aiInferredRemote = aiInferredRemote,
        )
}
