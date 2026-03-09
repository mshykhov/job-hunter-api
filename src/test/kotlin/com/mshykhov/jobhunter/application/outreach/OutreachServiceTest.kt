package com.mshykhov.jobhunter.application.outreach

/**
 * Unit tests for OutreachService (MockK-based).
 *
 * == generateCoverLetter ==
 * - should generate cover letter, save to userJob, and return response
 * - should throw NotFoundException when user doesn't exist
 * - should throw NotFoundException when job not in user's list
 * - should throw AiNotConfiguredException when no AI settings
 * - should use source-specific prompt when available
 * - should use default prompt when no source-specific config
 * - should pass user's about text to generator
 *
 * == generateRecruiterMessage ==
 * - should generate recruiter message, save to userJob, and return response
 * - should throw NotFoundException when user doesn't exist
 * - should throw NotFoundException when job not in user's list
 * - should throw AiNotConfiguredException when no AI settings
 * - should use source-specific prompt when available
 *
 * == testCoverLetter ==
 * - should generate cover letter without saving (read-only)
 * - should throw NotFoundException when no jobs for source
 * - should throw AiNotConfiguredException when no AI settings
 * - should use most recent job from source for testing
 *
 * == testRecruiterMessage ==
 * - should generate recruiter message without saving (read-only)
 * - should throw NotFoundException when no jobs for source
 *
 * == getSettings ==
 * - should return default settings when none saved
 * - should return saved settings
 *
 * == saveSettings ==
 * - should create settings for new user
 * - should update existing settings
 * - should apply source config correctly
 */
class OutreachServiceTest
