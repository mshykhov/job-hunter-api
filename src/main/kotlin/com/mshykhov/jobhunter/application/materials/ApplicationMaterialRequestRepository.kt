package com.mshykhov.jobhunter.application.materials

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface ApplicationMaterialRequestRepository : JpaRepository<ApplicationMaterialRequestEntity, UUID> {
    fun findByMaterialPackageIdAndIdempotencyKey(packageId: UUID, idempotencyKey: String): ApplicationMaterialRequestEntity?
    fun findAllByMaterialPackageIdOrderByCreatedAtDesc(packageId: UUID): List<ApplicationMaterialRequestEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ApplicationMaterialRequestEntity r where r.id = :id")
    fun findForUpdate(id: UUID): ApplicationMaterialRequestEntity?

    @Query(
        value =
        """
            SELECT * FROM application_material_requests
            WHERE status = 'QUEUED'
            ORDER BY created_at
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """,
        nativeQuery = true,
    )
    fun claimNext(): ApplicationMaterialRequestEntity?

    @Modifying
    @Query(
        value =
        """
            UPDATE application_material_requests
            SET status = 'QUEUED', lease_owner = NULL, lease_token = NULL, lease_expires_at = NULL, updated_at = now()
            WHERE status IN ('CLAIMED', 'GENERATING', 'VALIDATING', 'RENDERING')
              AND lease_expires_at < :now
              AND attempt_count < 3
            """,
        nativeQuery = true,
    )
    fun requeueExpired(now: Instant): Int

    @Modifying
    @Query(
        value =
        """
            UPDATE application_material_requests
            SET status = 'FAILED', lease_owner = NULL, lease_token = NULL, lease_expires_at = NULL, updated_at = now()
            WHERE status IN ('CLAIMED', 'GENERATING', 'VALIDATING', 'RENDERING')
              AND lease_expires_at < :now
              AND attempt_count >= 3
            """,
        nativeQuery = true,
    )
    fun failExhaustedExpired(now: Instant): Int
}
