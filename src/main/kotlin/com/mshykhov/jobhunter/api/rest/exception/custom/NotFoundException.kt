package com.mshykhov.jobhunter.api.rest.exception.custom

class NotFoundException(
    message: String,
) : RuntimeException(message)