package com.mshykhov.jobhunter.application.materials

import com.mshykhov.jobhunter.application.common.ConflictException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MaterialStateMachineTest {
    private val stateMachine = MaterialStateMachine()

    @ParameterizedTest
    @CsvSource(
        "QUEUED, CLAIMED",
        "CLAIMED, GENERATING",
        "GENERATING, VALIDATING",
        "VALIDATING, RENDERING",
        "RENDERING, READY",
        "RENDERING, READY_WITH_FALLBACK",
        "GENERATING, BLOCKED",
        "VALIDATING, BLOCKED",
        "RENDERING, BLOCKED",
        "CLAIMED, QUEUED",
        "GENERATING, QUEUED",
        "VALIDATING, QUEUED",
        "RENDERING, QUEUED",
    )
    fun `allows supported transition`(from: MaterialStatus, to: MaterialStatus) {
        assertDoesNotThrow { stateMachine.requireTransition(from, to) }
    }

    @ParameterizedTest
    @CsvSource(
        "QUEUED, READY",
        "READY, GENERATING",
        "READY_WITH_FALLBACK, READY",
        "BLOCKED, READY",
        "FAILED, QUEUED",
    )
    fun `rejects unsupported transition`(from: MaterialStatus, to: MaterialStatus) {
        assertThrows(ConflictException::class.java) { stateMachine.requireTransition(from, to) }
    }
}
