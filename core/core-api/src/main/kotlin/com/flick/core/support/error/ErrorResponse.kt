package com.flick.core.support.error

import java.time.Instant

data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
    val errors: List<FieldError>? = null,
) {
    constructor(type: ErrorType, errors: List<FieldError>? = null) : this(
        code = type.name,
        message = type.message,
        errors = errors,
    )
}
