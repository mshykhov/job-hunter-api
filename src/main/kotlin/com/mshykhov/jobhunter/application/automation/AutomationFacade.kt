package com.mshykhov.jobhunter.application.automation

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Transactional(readOnly = true)
class AutomationFacade(
    private val delegationRepository: AutomationDelegationRepository,
    private val runnerRepository: AutomationRunnerRepository,
    private val transitionRepository: AutomationRunnerTransitionRepository,
) {
    fun findActiveDelegation(
        ownerIssuer: String,
        ownerSubject: String,
    ): AutomationDelegationEntity? =
        delegationRepository.findByOwnerIssuerAndOwnerSubjectAndRevokedAtIsNull(ownerIssuer, ownerSubject)

    @Transactional
    fun saveDelegation(delegation: AutomationDelegationEntity): AutomationDelegationEntity =
        delegationRepository.save(delegation)

    fun findRunner(delegationId: UUID): AutomationRunnerEntity? = runnerRepository.findByDelegationId(delegationId)

    @Transactional
    fun saveRunner(runner: AutomationRunnerEntity): AutomationRunnerEntity = runnerRepository.save(runner)

    @Transactional
    fun appendTransitions(transitions: List<AutomationRunnerTransitionEntity>): List<AutomationRunnerTransitionEntity> =
        transitionRepository.saveAll(transitions)
}
