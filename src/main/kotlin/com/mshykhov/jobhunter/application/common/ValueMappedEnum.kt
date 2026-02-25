package com.mshykhov.jobhunter.application.common

import com.fasterxml.jackson.annotation.JsonValue

interface ValueMappedEnum {
    @get:JsonValue
    val value: String
}
