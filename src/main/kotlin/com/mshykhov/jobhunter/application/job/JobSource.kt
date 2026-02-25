package com.mshykhov.jobhunter.application.job

import com.fasterxml.jackson.annotation.JsonCreator
import com.mshykhov.jobhunter.application.common.ValueMappedEnum

enum class JobSource(
    override val value: String,
) : ValueMappedEnum {
    DOU("dou"),
    DJINNI("djinni"),
    INDEED("indeed"),
    LINKEDIN("linkedin"),
    ;

    override fun toString(): String = value

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): JobSource = entries.first { it.value.equals(value, ignoreCase = true) }
    }
}
