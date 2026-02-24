package com.mshykhov.jobhunter.persistence.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "jobs")
class JobEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(nullable = false)
    val title: String,
    @Column(nullable = false)
    val company: String,
    @Column(nullable = false, unique = true)
    val url: String,
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val source: JobSource,
    val salary: String? = null,
    val location: String? = null,
    @Column(nullable = false)
    val remote: Boolean = false,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: JobStatus = JobStatus.NEW,
    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
)
