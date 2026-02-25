package com.mshykhov.jobhunter.infrastructure.config

import com.mshykhov.jobhunter.application.common.ValueMappedEnum
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.converter.ConverterFactory
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverterFactory(ValueMappedEnumConverterFactory())
    }
}

class ValueMappedEnumConverterFactory : ConverterFactory<String, ValueMappedEnum> {
    override fun <T : ValueMappedEnum> getConverter(targetType: Class<T>): Converter<String, T> =
        Converter { source ->
            targetType.enumConstants.first { it.value.equals(source, ignoreCase = true) }
        }
}
