package com.mshykhov.jobhunter.api.rest.proxy.dto

import com.mshykhov.jobhunter.infrastructure.proxy.WebshareProxy

data class ProxyResponse(
    val url: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val countryCode: String,
) {
    companion object {
        fun from(proxy: WebshareProxy): ProxyResponse =
            ProxyResponse(
                url = "http://${proxy.username}:${proxy.password}@${proxy.proxyAddress}:${proxy.port}",
                host = proxy.proxyAddress,
                port = proxy.port,
                username = proxy.username,
                password = proxy.password,
                countryCode = proxy.countryCode,
            )
    }
}
