package com.mshykhov.jobhunter.application.preference

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Embeddable
class MatchingPreferences(
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "excluded_keywords", columnDefinition = "text[]")
    var excludedKeywords: List<String> = emptyList(),
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "excluded_title_keywords", columnDefinition = "text[]")
    var excludedTitleKeywords: List<String> = emptyList(),
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "excluded_companies", columnDefinition = "text[]")
    var excludedCompanies: List<String> = emptyList(),
    @Column(name = "match_with_ai", nullable = false)
    var matchWithAi: Boolean = true,
    @Column(name = "custom_prompt", columnDefinition = "TEXT")
    var customPrompt: String? = null,
)
