package com.mshykhov.jobhunter.application.job

import org.springframework.data.jpa.domain.Specification
import java.time.Instant

object JobSpecifications {
    fun search(query: String): Specification<JobEntity> =
        Specification { root, _, cb ->
            val pattern = "%${query.lowercase()}%"
            cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("company")), pattern),
                cb.like(cb.lower(root.get("location")), pattern),
            )
        }

    fun source(source: JobSource): Specification<JobEntity> =
        Specification { root, _, cb ->
            cb.equal(root.get<JobSource>("source"), source)
        }

    fun remote(): Specification<JobEntity> =
        Specification { root, _, cb ->
            cb.equal(root.get<Boolean>("remote"), true)
        }

    fun publishedAfter(instant: Instant): Specification<JobEntity> =
        Specification { root, _, cb ->
            cb.greaterThanOrEqualTo(root.get("publishedAt"), instant)
        }
}
