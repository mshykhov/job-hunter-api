package com.mshykhov.jobhunter.application.preference

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Embeddable
class MatchingPreferences(
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "keywords", columnDefinition = "text[]")
    var keywords: List<String> = emptyList(),
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "excluded_keywords", columnDefinition = "text[]")
    var excludedKeywords: List<String> = emptyList(),
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "excluded_title_keywords", columnDefinition = "text[]")
    var excludedTitleKeywords: List<String> = emptyList(),
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "excluded_companies", columnDefinition = "text[]")
    var excludedCompanies: List<String> = emptyList(),
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "seniority_levels", columnDefinition = "text[]")
    var seniorityLevels: List<String> = emptyList(),
    @Column(name = "match_with_ai", nullable = false)
    var matchWithAi: Boolean = true,
    @Column(name = "custom_prompt", columnDefinition = "TEXT")
    var customPrompt: String? = null,
    @Column(name = "weight_keywords", nullable = false)
    var weightKeywords: Int = DEFAULT_WEIGHT_KEYWORDS,
    @Column(name = "weight_seniority", nullable = false)
    var weightSeniority: Int = DEFAULT_WEIGHT_SENIORITY,
    @Column(name = "weight_categories", nullable = false)
    var weightCategories: Int = DEFAULT_WEIGHT_CATEGORIES,
) {
    companion object {
        const val DEFAULT_WEIGHT_KEYWORDS = 45
        const val DEFAULT_WEIGHT_SENIORITY = 30
        const val DEFAULT_WEIGHT_CATEGORIES = 25
    }
}
