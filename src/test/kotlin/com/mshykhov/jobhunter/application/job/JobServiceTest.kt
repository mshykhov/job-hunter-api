package com.mshykhov.jobhunter.application.job

/**
 * Unit tests for JobService (MockK-based).
 *
 * == ingest ==
 * - should create new job when URL doesn't exist
 * - should update existing job when fields changed
 * - should not update when all fields are identical (unchanged)
 * - should deduplicate requests by URL (last one wins)
 * - should parse publishedAt in ISO-8601 format
 * - should parse publishedAt in space-separated offset format
 * - should keep existing publishedAt when new value is unparseable
 * - should handle empty request list
 * - should handle mix of new, updated, and unchanged jobs
 * - should save only changed entities (not unchanged)
 *
 * == checkJobs ==
 * - should classify brand new URL as "new"
 * - should classify existing URL with no field changes as "unchanged"
 * - should classify existing URL with title change as "updated"
 * - should classify existing URL with salary change as "updated"
 * - should classify existing URL with location change as "updated"
 * - should classify existing URL with publishedAt change as "updated"
 * - should ignore null fields in check request (only compare non-null)
 * - should return all-empty lists for empty input
 * - should deduplicate check requests by URL
 *
 * == searchPublic ==
 * - should build spec with search filter
 * - should build spec with sources filter
 * - should build spec with remote filter
 * - should build spec with publishedAfter filter
 * - should combine multiple filters
 * - should clamp page size to max 100
 * - should clamp page to minimum 0
 */
class JobServiceTest
