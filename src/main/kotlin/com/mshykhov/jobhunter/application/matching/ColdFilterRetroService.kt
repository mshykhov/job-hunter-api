package com.mshykhov.jobhunter.application.matching

import com.mshykhov.jobhunter.application.preference.PreferenceChangedEvent
import com.mshykhov.jobhunter.application.preference.UserPreferenceFacade
import com.mshykhov.jobhunter.application.userjob.UserJobGroupFacade
import com.mshykhov.jobhunter.application.userjob.UserJobStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionalEventListener

private val logger = KotlinLogging.logger {}

@Service
class ColdFilterRetroService(private val userPreferenceFacade: UserPreferenceFacade, private val userJobGroupFacade: UserJobGroupFacade) {
    private val coldFilterChain = ColdFilterChain()

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onPreferenceChanged(event: PreferenceChangedEvent) {
        val preference = userPreferenceFacade.findByUserId(event.userId) ?: return
        val rejected =
            userJobGroupFacade
                .findByUserIdAndStatus(event.userId, UserJobStatus.NEW)
                .filter { userJobGroup ->
                    val representative = userJobGroup.group.jobs.maxByOrNull { it.description.length }
                    representative != null &&
                        coldFilterChain.evaluate(representative, preference) is FilterResult.Rejected
                }
        if (rejected.isEmpty()) return

        userJobGroupFacade.deleteAll(rejected)
        logger.info { "Retro cold filter removed ${rejected.size} NEW groups for user ${event.userId}" }
    }
}
