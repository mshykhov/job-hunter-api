package com.mshykhov.jobhunter.application.criteria

/**
 * Unit tests for SearchCriteriaService (MockK-based).
 *
 * == getAggregated ==
 * - should return empty criteria when no preferences exist
 * - should aggregate categories from all users not disabling source
 * - should aggregate locations from all users not disabling source
 * - should deduplicate categories and locations
 * - should exclude users who disabled the given source
 * - should set remoteOnly=true when ALL users want remote for this source
 * - should set remoteOnly=false when at least one user doesn't want remote
 * - should set remoteOnly=false when preferences list is empty
 * - should handle single user with preferences
 * - should handle multiple users with overlapping categories
 */
class SearchCriteriaServiceTest
