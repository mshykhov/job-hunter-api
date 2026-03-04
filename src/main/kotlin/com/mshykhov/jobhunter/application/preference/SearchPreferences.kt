package com.mshykhov.jobhunter.application.preference

import com.mshykhov.jobhunter.application.job.JobSource
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Embeddable
class SearchPreferences(
    @Column(name = "raw_input", columnDefinition = "TEXT")
    var rawInput: String? = null,
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "categories", columnDefinition = "text[]")
    var categories: List<String> = emptyList(),
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "locations", columnDefinition = "text[]")
    var locations: List<String> = emptyList(),
    @Column(name = "remote_only", nullable = false)
    var remoteOnly: Boolean = false,
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Enumerated(EnumType.STRING)
    @Column(name = "disabled_sources", columnDefinition = "text[]")
    var disabledSources: List<JobSource> = emptyList(),
)
