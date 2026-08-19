package com.mshykhov.jobhunter.application.automation

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AutomationDelegationRepository : JpaRepository<AutomationDelegationEntity, UUID> {
    fun findByOwnerIssuerAndOwnerSubject(
        ownerIssuer: String,
        ownerSubject: String,
    ): AutomationDelegationEntity?

    fun findByOwnerIssuerAndOwnerSubjectAndRevokedAtIsNull(
        ownerIssuer: String,
        ownerSubject: String,
    ): AutomationDelegationEntity?
}
