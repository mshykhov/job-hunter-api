package com.mshykhov.jobhunter.persistence.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.mshykhov.jobhunter.common.enums.ValueMappedEnum

enum class JobSource(
    override val value: String,
) : ValueMappedEnum {
    DOU("dou"),
    DJINNI("djinni"),
    INDEED("indeed"),
    ;

    override fun toString(): String = value

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): JobSource = entries.first { it.value.equals(value, ignoreCase = true) }
    }
}
