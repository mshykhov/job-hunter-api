package com.mshykhov.jobhunter.application.outreach

import com.mshykhov.jobhunter.api.rest.job.dto.CoverLetterResponse
import com.mshykhov.jobhunter.api.rest.job.dto.RecruiterMessageResponse
import com.mshykhov.jobhunter.api.rest.settings.dto.OutreachSettingsResponse
import com.mshykhov.jobhunter.api.rest.settings.dto.SaveOutreachSettingsRequest
import com.mshykhov.jobhunter.application.ai.ChatClientFactory
import com.mshykhov.jobhunter.application.ai.OutreachGenerator
import com.mshykhov.jobhunter.application.ai.UserAiSettingsService
import com.mshykhov.jobhunter.application.common.NotFoundException
import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobFacade
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.preference.UserPreferenceFacade
import com.mshykhov.jobhunter.application.user.UserFacade
import com.mshykhov.jobhunter.application.userjob.UserJobEntity
import com.mshykhov.jobhunter.application.userjob.UserJobFacade
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OutreachService(
    private val userFacade: UserFacade,
    private val userJobFacade: UserJobFacade,
    private val jobFacade: JobFacade,
    private val userPreferenceFacade: UserPreferenceFacade,
    private val outreachSettingsFacade: OutreachSettingsFacade,
    private val outreachGenerator: OutreachGenerator,
    private val userAiSettingsService: UserAiSettingsService,
    private val chatClientFactory: ChatClientFactory,
) {
    @Transactional(readOnly = true)
    fun getSettings(auth0Sub: String): OutreachSettingsResponse {
        val user = userFacade.findByAuth0Sub(auth0Sub) ?: return OutreachSettingsResponse.from(null)
        val entity = outreachSettingsFacade.findByUserId(user.id)
        return OutreachSettingsResponse.from(entity)
    }

    @Transactional
    fun saveSettings(
        auth0Sub: String,
        request: SaveOutreachSettingsRequest,
    ): OutreachSettingsResponse {
        val user = userFacade.findOrCreate(auth0Sub)
        val entity =
            outreachSettingsFacade
                .findByUserId(user.id)
                ?.also { request.applyTo(it) }
                ?: request.toEntity(user)
        return OutreachSettingsResponse.from(outreachSettingsFacade.save(entity))
    }

    @Transactional
    fun generateCoverLetter(
        auth0Sub: String,
        jobId: UUID,
    ): CoverLetterResponse {
        val context = resolveGenerationContext(auth0Sub, jobId)
        val coverLetter =
            outreachGenerator.generateCoverLetter(
                context.job,
                context.about,
                context.sourceConfig,
                context.settings?.coverLetterPrompt,
                context.chatClient,
            )
        context.userJob.coverLetter = coverLetter
        userJobFacade.save(context.userJob)
        return CoverLetterResponse.of(coverLetter, context.job)
    }

    @Transactional
    fun generateRecruiterMessage(
        auth0Sub: String,
        jobId: UUID,
    ): RecruiterMessageResponse {
        val context = resolveGenerationContext(auth0Sub, jobId)
        val recruiterMessage =
            outreachGenerator.generateRecruiterMessage(
                context.job,
                context.about,
                context.sourceConfig,
                context.settings?.recruiterMessagePrompt,
                context.chatClient,
            )
        context.userJob.recruiterMessage = recruiterMessage
        userJobFacade.save(context.userJob)
        return RecruiterMessageResponse.of(recruiterMessage, context.job)
    }

    @Transactional(readOnly = true)
    fun testCoverLetter(
        auth0Sub: String,
        source: JobSource,
    ): CoverLetterResponse {
        val context = resolveTestContext(auth0Sub, source)
        val coverLetter =
            outreachGenerator.generateCoverLetter(
                context.job,
                context.about,
                context.sourceConfig,
                context.settings?.coverLetterPrompt,
                context.chatClient,
            )
        return CoverLetterResponse.of(coverLetter, context.job)
    }

    @Transactional(readOnly = true)
    fun testRecruiterMessage(
        auth0Sub: String,
        source: JobSource,
    ): RecruiterMessageResponse {
        val context = resolveTestContext(auth0Sub, source)
        val recruiterMessage =
            outreachGenerator.generateRecruiterMessage(
                context.job,
                context.about,
                context.sourceConfig,
                context.settings?.recruiterMessagePrompt,
                context.chatClient,
            )
        return RecruiterMessageResponse.of(recruiterMessage, context.job)
    }

    private fun resolveTestContext(
        auth0Sub: String,
        source: JobSource,
    ): TestContext {
        val user =
            userFacade.findByAuth0Sub(auth0Sub)
                ?: throw NotFoundException("User not found")
        val job =
            jobFacade.findTopBySourceOrderByCreatedAtDesc(source)
                ?: throw NotFoundException("No jobs found for source ${source.displayName}")
        val settings = outreachSettingsFacade.findByUserId(user.id)
        val sourceConfig = settings?.sourceConfig?.get(source.name)
        val about = userPreferenceFacade.findByUserId(user.id)?.about
        val aiSettings = userAiSettingsService.resolveForUser(auth0Sub)
        val chatClient = chatClientFactory.createForUser(aiSettings)
        return TestContext(job, settings, sourceConfig, about, chatClient)
    }

    private fun resolveGenerationContext(
        auth0Sub: String,
        jobId: UUID,
    ): GenerationContext {
        val user =
            userFacade.findByAuth0Sub(auth0Sub)
                ?: throw NotFoundException("User not found")
        val userJob =
            userJobFacade.findByUserIdAndJobId(user.id, jobId)
                ?: throw NotFoundException("Job not found in your matches")
        val job = userJob.job
        val settings = outreachSettingsFacade.findByUserId(user.id)
        val sourceConfig = settings?.sourceConfig?.get(job.source.name)
        val about = userPreferenceFacade.findByUserId(user.id)?.about
        val aiSettings = userAiSettingsService.resolveForUser(auth0Sub)
        val chatClient = chatClientFactory.createForUser(aiSettings)
        return GenerationContext(userJob, job, settings, sourceConfig, about, chatClient)
    }
}

private class GenerationContext(
    val userJob: UserJobEntity,
    val job: JobEntity,
    val settings: OutreachSettingsEntity?,
    val sourceConfig: OutreachSourceConfig?,
    val about: String?,
    val chatClient: ChatClient,
)

private class TestContext(
    val job: JobEntity,
    val settings: OutreachSettingsEntity?,
    val sourceConfig: OutreachSourceConfig?,
    val about: String?,
    val chatClient: ChatClient,
)
