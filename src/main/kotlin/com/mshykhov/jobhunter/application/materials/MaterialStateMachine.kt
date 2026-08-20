package com.mshykhov.jobhunter.application.materials

import com.mshykhov.jobhunter.application.common.ConflictException
import org.springframework.stereotype.Component

@Component
class MaterialStateMachine {
    fun requireTransition(from: MaterialStatus, to: MaterialStatus) {
        if (to !in ALLOWED_TRANSITIONS.getValue(from)) {
            throw ConflictException("Unsupported material transition: $from -> $to")
        }
    }

    private companion object {
        val ALLOWED_TRANSITIONS =
            mapOf(
                MaterialStatus.QUEUED to setOf(MaterialStatus.CLAIMED, MaterialStatus.FAILED),
                MaterialStatus.CLAIMED to
                    setOf(MaterialStatus.GENERATING, MaterialStatus.QUEUED, MaterialStatus.FAILED),
                MaterialStatus.GENERATING to
                    setOf(
                        MaterialStatus.VALIDATING,
                        MaterialStatus.QUEUED,
                        MaterialStatus.BLOCKED,
                        MaterialStatus.FAILED,
                    ),
                MaterialStatus.VALIDATING to
                    setOf(
                        MaterialStatus.RENDERING,
                        MaterialStatus.QUEUED,
                        MaterialStatus.BLOCKED,
                        MaterialStatus.FAILED,
                    ),
                MaterialStatus.RENDERING to
                    setOf(
                        MaterialStatus.READY,
                        MaterialStatus.READY_WITH_FALLBACK,
                        MaterialStatus.QUEUED,
                        MaterialStatus.BLOCKED,
                        MaterialStatus.FAILED,
                    ),
                MaterialStatus.READY to emptySet(),
                MaterialStatus.READY_WITH_FALLBACK to emptySet(),
                MaterialStatus.BLOCKED to emptySet(),
                MaterialStatus.FAILED to emptySet(),
            )
    }
}
