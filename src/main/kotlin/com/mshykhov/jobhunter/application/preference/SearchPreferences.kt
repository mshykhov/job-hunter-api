package com.mshykhov.jobhunter.application.preference

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
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
    @Column(name = "seniority_levels", columnDefinition = "text[]")
    var seniorityLevels: List<String> = emptyList(),
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "locations", columnDefinition = "text[]")
    var locations: List<String> = emptyList(),
    @Column(name = "remote_only", nullable = false)
    var remoteOnly: Boolean = false,
)
