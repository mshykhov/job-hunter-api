package com.mshykhov.jobhunter.api.rest.criteria

import com.mshykhov.jobhunter.support.AbstractIntegrationTest

/**
 * Integration tests for SearchCriteriaController (/criteria).
 *
 * == GET /criteria?source={source} ==
 * - should return empty criteria when no users have preferences
 * - should aggregate categories from multiple users for given source
 * - should aggregate locations from multiple users for given source
 * - should deduplicate categories and locations
 * - should set remoteOnly=true only when ALL users require remote for this source
 * - should set remoteOnly=false when at least one user doesn't require remote
 * - should exclude users who disabled the given source
 * - should return 400 when source parameter is missing
 * - should return 400 for invalid source value
 * - should work for each JobSource (DOU, DJINNI, LINKEDIN, EUREMOTEJOBS, WEB3CAREER)
 */
class SearchCriteriaControllerIntegrationTest : AbstractIntegrationTest()
