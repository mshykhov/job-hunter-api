package com.mshykhov.jobhunter.common.enums

import com.fasterxml.jackson.annotation.JsonValue

interface ValueMappedEnum {
    @get:JsonValue
    val value: String
}
