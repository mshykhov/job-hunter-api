package com.mshykhov.jobhunter.application.automation

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select d from AutomationDelegationEntity d
        where d.ownerIssuer = :ownerIssuer and d.ownerSubject = :ownerSubject and d.revokedAt is null
        """,
    )
    fun findActiveForUpdate(
        ownerIssuer: String,
        ownerSubject: String,
    ): AutomationDelegationEntity?
}
