package com.mshykhov.jobhunter.application.statistics

import com.mshykhov.jobhunter.application.job.JobEntity
import com.mshykhov.jobhunter.application.job.JobGroupEntity
import com.mshykhov.jobhunter.application.user.UserEntity
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Component
class UserJobGroupDecisionFacade(private val repository: UserJobGroupDecisionRepository, private val clock: Clock) {
    @Transactional
    fun upsert(
        user: UserEntity,
        group: JobGroupEntity,
        jobs: List<JobEntity>,
        outcome: DecisionOutcome,
        coldFilter: String? = null,
        aiScore: Int? = null,
        inferredRemote: Boolean? = null,
    ) {
        require(aiScore == null || aiScore in 0..100) { "AI score must be between 0 and 100" }
        val sources = jobs.map { it.source.name }.distinct().sorted().toTypedArray()
        val categories = group.categories.map { it.value }.sorted().toTypedArray()
        val decidedAt = Instant.now(clock)
        val existing = repository.findByUserIdAndGroupId(user.id, group.id)
        if (existing == null) {
            repository.save(
                UserJobGroupDecisionEntity(
                    user = user,
                    group = group,
                    vacancySeenAt = requireNotNull(group.createdAt),
                    decidedAt = decidedAt,
                    outcome = outcome,
                    coldFilter = coldFilter?.take(100),
                    aiScore = aiScore,
                    inferredRemote = inferredRemote,
                    sources = sources,
                    categories = categories,
                ),
            )
        } else {
            existing.update(outcome, coldFilter, aiScore, inferredRemote, sources, categories, decidedAt)
        }
    }
}
