package com.mshykhov.jobhunter.api.rest

import org.springframework.boot.info.BuildProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class VersionController(private val buildProperties: BuildProperties) {
    @GetMapping("/public/version")
    fun getVersion(): VersionResponse =
        VersionResponse(
            version = buildProperties.version,
            name = buildProperties.name,
        )
}

data class VersionResponse(val version: String, val name: String)
