package com.mshykhov.jobhunter.application.job

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@JvmInline
value class Category private constructor(@JsonValue val value: String) {
    init {
        require(value.isNotBlank()) { "Category must not be blank" }
    }

    companion object {
        @JvmStatic
        @JsonCreator
        operator fun invoke(input: String): Category = Category(input.trim().lowercase())

        val String.category: Category get() = invoke(this)
    }

    override fun toString(): String = value
}
