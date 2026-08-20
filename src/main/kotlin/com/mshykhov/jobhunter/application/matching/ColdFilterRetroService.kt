package com.mshykhov.jobhunter.application.matching

import com.mshykhov.jobhunter.application.preference.PreferenceChangedEvent
import com.mshykhov.jobhunter.application.preference.UserPreferenceFacade
import com.mshykhov.jobhunter.application.statistics.DecisionOutcome
import com.mshykhov.jobhunter.application.statistics.UserJobGroupDecisionFacade
import com.mshykhov.jobhunter.application.userjob.UserJobGroupFacade
import com.mshykhov.jobhunter.application.userjob.UserJobStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityManager
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionalEventListener

private val logger = KotlinLogging.logger {}
internal const val COLD_FILTER_RETRO_BATCH_SIZE = 100
const val COLD_FILTER_RETRO_EXECUTOR = "coldFilterRetroExecutor"

@Service
class ColdFilterRetroService(
    private val userPreferenceFacade: UserPreferenceFacade,
    private val userJobGroupFacade: UserJobGroupFacade,
    private val decisionFacade: UserJobGroupDecisionFacade,
    private val entityManager: EntityManager,
) {
    private val coldFilterChain = ColdFilterChain()

    @Async(COLD_FILTER_RETRO_EXECUTOR)
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onPreferenceChanged(event: PreferenceChangedEvent) {
        try {
            val preference = userPreferenceFacade.findByUserId(event.userId) ?: return
            val groupIds = userJobGroupFacade.findIdsByUserIdAndStatus(event.userId, UserJobStatus.NEW)
            var removedCount = 0

            groupIds.chunked(COLD_FILTER_RETRO_BATCH_SIZE).forEach { batchIds ->
                try {
                    val rejected =
                        userJobGroupFacade
                            .findByIdsWithGroupAndJobs(event.userId, UserJobStatus.NEW, batchIds)
                            .filter { userJobGroup ->
                                val representative = userJobGroup.group.jobs.maxByOrNull { it.description.length }
                                representative != null &&
                                    coldFilterChain.evaluate(representative, preference) is FilterResult.Rejected
                            }

                    rejected.forEach { userJobGroup ->
                        val representative = requireNotNull(userJobGroup.group.jobs.maxByOrNull { it.description.length })
                        val filterResult = coldFilterChain.evaluate(representative, preference) as FilterResult.Rejected
                        decisionFacade.upsert(
                            userJobGroup.user,
                            userJobGroup.group,
                            userJobGroup.group.jobs,
                            DecisionOutcome.COLD_REJECTED,
                            coldFilter = filterResult.filter,
                        )
                    }
                    if (rejected.isNotEmpty()) {
                        removedCount +=
                            userJobGroupFacade.deleteByIdsAndUserIdAndStatus(
                                rejected.map { it.id },
                                event.userId,
                                UserJobStatus.NEW,
                            )
                    }
                    userJobGroupFacade.flush()
                } finally {
                    entityManager.clear()
                }
            }

            logger.info {
                "Retro cold filter processed ${groupIds.size} NEW groups and removed $removedCount for user ${event.userId}"
            }
        } catch (exception: Exception) {
            logger.error(exception) { "Retro cold filter failed for user ${event.userId}" }
            throw exception
        }
    }
}
