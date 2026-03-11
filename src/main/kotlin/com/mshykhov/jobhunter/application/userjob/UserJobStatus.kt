package com.mshykhov.jobhunter.application.userjob

import com.fasterxml.jackson.annotation.JsonCreator
import com.mshykhov.jobhunter.application.common.ValueMappedEnum

enum class UserJobStatus(override val value: String, val displayName: String) : ValueMappedEnum {
    NEW("new", "New"),
    APPLIED("applied", "Applied"),
    IRRELEVANT("irrelevant", "Irrelevant"),
    ;

    override fun toString(): String = value

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): UserJobStatus = entries.first { it.value.equals(value, ignoreCase = true) }
    }
}
