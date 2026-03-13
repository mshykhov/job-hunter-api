package com.mshykhov.jobhunter.application.preference

import com.mshykhov.jobhunter.application.job.Category
import com.mshykhov.jobhunter.application.job.JobSource
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Embeddable
class SearchPreferences(
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "categories", columnDefinition = "jsonb")
    var categories: Set<Category> = emptySet(),
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
