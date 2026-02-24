package com.mshykhov.jobhunter.exception

import java.util.UUID

class JobNotFoundException(
    id: UUID,
) : RuntimeException("Job not found: $id")
