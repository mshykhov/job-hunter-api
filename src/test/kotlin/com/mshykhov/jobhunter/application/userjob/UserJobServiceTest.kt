package com.mshykhov.jobhunter.application.userjob

/**
 * Unit tests for UserJobService (MockK-based).
 *
 * == search ==
 * - should return empty response when user doesn't exist
 * - should build spec with status filter
 * - should build spec with source filter
 * - should build spec with remote filter
 * - should build spec with minScore filter
 * - should build spec with search text filter
 * - should build spec with publishedAfter filter
 * - should build spec with matchedAfter filter
 * - should build spec with updatedAfter filter
 * - should combine multiple filters
 * - should include statusCounts for all UserJobStatus values
 * - should clamp page size to max 100
 * - should apply correct sort order (SCORE, PUBLISHED, SCRAPED, MATCHED)
 *
 * == getDetail ==
 * - should return user job entity when found
 * - should throw NotFoundException when user doesn't exist
 * - should throw NotFoundException when job not in user's list
 *
 * == updateStatus ==
 * - should update status and save
 * - should throw NotFoundException when user doesn't exist
 * - should throw NotFoundException when job not in user's list
 * - should allow transitions: NEW->APPLIED, NEW->IRRELEVANT, APPLIED->IRRELEVANT, etc.
 *
 * == bulkUpdateStatus ==
 * - should update status for all provided job IDs
 * - should silently skip job IDs not belonging to user
 * - should throw NotFoundException when user doesn't exist
 * - should handle empty jobIds list (returns empty)
 */
class UserJobServiceTest
