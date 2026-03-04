package com.mshykhov.jobhunter.application.preference

import com.mshykhov.jobhunter.application.job.JobSource
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
    @Enumerated(EnumType.STRING)
    @Column(name = "disabled_sources", columnDefinition = "text[]")
    var disabledSources: List<JobSource> = emptyList(),
    @Column(name = "min_score", nullable = false)
    var minScore: Int = 50,
    @Column(name = "match_with_ai", nullable = false)
    var matchWithAi: Boolean = true,
    @Column(name = "custom_prompt", columnDefinition = "TEXT")
    var customPrompt: String? = null,
)
