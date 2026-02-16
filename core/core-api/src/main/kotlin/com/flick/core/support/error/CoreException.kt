package com.flick.core.support.error

class CoreException(
    val type: ErrorType,
    val errors: List<FieldError>? = null,
) : RuntimeException(type.message)
