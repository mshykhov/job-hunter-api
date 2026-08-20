package com.mshykhov.jobhunter.infrastructure.materials

class MaterialDecryptionException(cause: Throwable? = null) : IllegalStateException("Application material authentication failed", cause)
