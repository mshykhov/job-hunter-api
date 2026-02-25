package com.mshykhov.jobhunter.api.rest.proxy

import com.mshykhov.jobhunter.api.rest.proxy.dto.ProxyResponse
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.proxy.ProxyService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/proxies")
class ProxyController(
    private val proxyService: ProxyService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_read:proxies')")
    fun get(
        @RequestParam source: JobSource,
    ): ProxyResponse = proxyService.getProxy(source)
}
