package com.mshykhov.jobhunter.api.rest.proxy

import com.mshykhov.jobhunter.support.AbstractIntegrationTest

/**
 * Integration tests for ProxyController (/proxies).
 *
 * Note: Proxy service depends on external Webshare API (disabled in tests).
 * These tests verify endpoint routing, auth, and error handling.
 *
 * == GET /proxies?source={source} ==
 * - should return 503 when proxy service is disabled/unavailable
 * - should return 400 when source parameter is missing
 * - should return 400 for invalid source value
 *
 * == GET /proxies/all ==
 * - should return 503 when proxy service is disabled/unavailable
 */
class ProxyControllerIntegrationTest : AbstractIntegrationTest()
