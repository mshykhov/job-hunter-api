package com.mshykhov.jobhunter.application.userjob

import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.user.UserEntity
import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import java.time.Instant
import java.util.UUID

object UserJobSpecifications {
    fun userId(userId: UUID): Specification<UserJobEntity> =
        Specification { root, _, cb ->
            cb.equal(root.get<UserEntity>("user").get<UUID>("id"), userId)
        }

    fun statuses(statuses: List<UserJobStatus>): Specification<UserJobEntity> =
        Specification { root, _, _ ->
            root.get<UserJobStatus>("status").`in`(statuses)
        }

    fun sources(sources: List<JobSource>): Specification<UserJobEntity> =
        Specification { root, _, _ ->
            root.get<JobEntity>("job").get<JobSource>("source").`in`(sources)
        }

    fun publishedAfter(instant: Instant): Specification<UserJobEntity> =
        Specification { root, _, cb ->
            cb.greaterThanOrEqualTo(root.get<JobEntity>("job").get("publishedAt"), instant)
        }

    fun matchedAfter(instant: Instant): Specification<UserJobEntity> =
        Specification { root, _, cb ->
            cb.greaterThanOrEqualTo(root.get("createdAt"), instant)
        }

    fun updatedAfter(instant: Instant): Specification<UserJobEntity> =
        Specification { root, _, cb ->
            cb.greaterThanOrEqualTo(root.get<JobEntity>("job").get("updatedAt"), instant)
        }

    fun remote(): Specification<UserJobEntity> =
        Specification { root, _, cb ->
            cb.or(
                cb.equal(root.get<JobEntity>("job").get<Boolean>("remote"), true),
                cb.equal(root.get<Boolean>("aiInferredRemote"), true),
            )
        }

    fun minScore(score: Int): Specification<UserJobEntity> =
        Specification { root, _, cb ->
            cb.greaterThanOrEqualTo(root.get("aiRelevanceScore"), score)
        }

    fun search(query: String): Specification<UserJobEntity> =
        Specification { root, _, cb ->
            val pattern = "%${query.lowercase()}%"
            val job = root.get<JobEntity>("job")
            cb.or(
                cb.like(cb.lower(job.get("title")), pattern),
                cb.like(cb.lower(job.get("company")), pattern),
                cb.like(cb.lower(job.get("location")), pattern),
                cb.like(cb.lower(job.get("salary")), pattern),
            )
        }

    fun withJobFetch(): Specification<UserJobEntity> =
        Specification { root, query, _ ->
            val resultType = query?.resultType
            if (resultType != Long::class.java && resultType != Long::class.javaObjectType) {
                root.fetch<UserJobEntity, JobEntity>("job", JoinType.LEFT)
            }
            null
        }
}
