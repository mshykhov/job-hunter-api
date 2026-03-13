package com.mshykhov.jobhunter.application.job

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.SerializerProvider

class JobSourceKeySerializer : JsonSerializer<JobSource>() {
    override fun serialize(
        value: JobSource,
        gen: JsonGenerator,
        serializers: SerializerProvider,
    ) {
        gen.writeFieldName(value.name)
    }
}

class JobSourceKeyDeserializer : KeyDeserializer() {
    override fun deserializeKey(
        key: String,
        ctxt: DeserializationContext,
    ): JobSource = JobSource.valueOf(key)
}
