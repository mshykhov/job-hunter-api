package com.mshykhov.jobhunter.persistence.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.mshykhov.jobhunter.common.enums.ValueMappedEnum

enum class UserJobStatus(
    override val value: String,
) : ValueMappedEnum {
    NEW("new"),
    APPLIED("applied"),
    IRRELEVANT("irrelevant"),
    ;

    override fun toString(): String = value

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): UserJobStatus = entries.first { it.value.equals(value, ignoreCase = true) }
    }
}
