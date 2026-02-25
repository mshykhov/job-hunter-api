package com.mshykhov.jobhunter.api.rest.exception.custom

class ServiceUnavailableException(
    message: String,
) : RuntimeException(message)
