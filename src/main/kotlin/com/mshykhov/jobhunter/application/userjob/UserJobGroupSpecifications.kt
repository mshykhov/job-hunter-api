package com.mshykhov.jobhunter.application.userjob

import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobGroupEntity
import com.mshykhov.jobhunter.application.job.JobSource
import com.mshykhov.jobhunter.application.user.UserEntity
import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import java.time.Instant
import java.util.UUID

object UserJobGroupSpecifications {
    fun userId(userId: UUID): Specification<UserJobGroupEntity> =
        Specification { root, _, cb ->
            cb.equal(root.get<UserEntity>("user").get<UUID>("id"), userId)
        }

    fun statuses(statuses: List<UserJobStatus>): Specification<UserJobGroupEntity> =
        Specification { root, _, _ ->
            root.get<UserJobStatus>("status").`in`(statuses)
        }

    fun minScore(score: Int): Specification<UserJobGroupEntity> =
        Specification { root, _, cb ->
            cb.greaterThanOrEqualTo(root.get("aiRelevanceScore"), score)
        }

    fun remote(): Specification<UserJobGroupEntity> =
        Specification { root, query, cb ->
            val subquery = query!!.subquery(UUID::class.java)
            val job = subquery.from(JobEntity::class.java)
            subquery.select(job.get<JobGroupEntity>("group").get("id"))
            subquery.where(
                cb.and(
                    cb.equal(job.get<JobGroupEntity>("group").get<UUID>("id"), root.get<JobGroupEntity>("group").get<UUID>("id")),
                    cb.equal(job.get<Boolean>("remote"), true),
                ),
            )
            cb.exists(subquery)
        }

    fun sources(sources: List<JobSource>): Specification<UserJobGroupEntity> =
        Specification { root, query, cb ->
            val subquery = query!!.subquery(UUID::class.java)
            val job = subquery.from(JobEntity::class.java)
            subquery.select(job.get<JobGroupEntity>("group").get("id"))
            subquery.where(
                cb.and(
                    cb.equal(job.get<JobGroupEntity>("group").get<UUID>("id"), root.get<JobGroupEntity>("group").get<UUID>("id")),
                    job.get<JobSource>("source").`in`(sources),
                ),
            )
            cb.exists(subquery)
        }

    fun matchedAfter(instant: Instant): Specification<UserJobGroupEntity> =
        Specification { root, _, cb ->
            cb.greaterThanOrEqualTo(root.get("createdAt"), instant)
        }

    fun search(query: String): Specification<UserJobGroupEntity> =
        Specification { root, _, cb ->
            val escaped =
                query
                    .lowercase()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_")
            val pattern = "%$escaped%"
            val group = root.get<JobGroupEntity>("group")
            cb.or(
                cb.like(cb.lower(group.get("title")), pattern, '\\'),
                cb.like(cb.lower(group.get("company")), pattern, '\\'),
            )
        }

    fun withGroupFetch(): Specification<UserJobGroupEntity> =
        Specification { root, query, _ ->
            val resultType = query?.resultType
            if (resultType != Long::class.java && resultType != Long::class.javaObjectType) {
                root.fetch<UserJobGroupEntity, JobGroupEntity>("group", JoinType.LEFT)
            }
            null
        }
}
