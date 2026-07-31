package com.mshykhov.jobhunter.application.matching

import com.mshykhov.jobhunter.application.job.JobFacade
import org.springframework.stereotype.Component
import java.util.function.Supplier

@Component
class MatchingBacklogSupplier(private val jobFacade: JobFacade) : Supplier<Long> {
    override fun get(): Long = jobFacade.countUnmatched()
}
