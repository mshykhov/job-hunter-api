package com.mshykhov.jobhunter.exception

class DuplicateJobException(
    url: String,
) : RuntimeException("Job already exists: $url")
