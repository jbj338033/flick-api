package com.flick.core.api.controller

import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorResponse
import com.flick.core.support.error.ErrorType
import com.flick.core.support.error.FieldError
import com.flick.support.logging.logger
import org.springframework.boot.logging.LogLevel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiControllerAdvice {
    private val log = logger()

    @ExceptionHandler(CoreException::class)
    fun handleCoreException(e: CoreException): ResponseEntity<ErrorResponse> {
        val msg = "[${e.type.name}] ${e.type.message}"

        when (e.type.logLevel) {
            LogLevel.ERROR -> log.error(e) { msg }
            LogLevel.WARN -> log.warn { msg }
            else -> log.info { msg }
        }

        return ResponseEntity
            .status(e.type.status)
            .body(ErrorResponse(e.type, e.errors))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors =
            e.bindingResult.fieldErrors.map {
                FieldError(it.field, it.defaultMessage ?: "Invalid value")
            }

        log.warn { "Validation failed: $errors" }

        return ResponseEntity
            .status(ErrorType.VALIDATION_ERROR.status)
            .body(ErrorResponse(ErrorType.VALIDATION_ERROR, errors))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        log.error(e) { "Unexpected error: ${e.message}" }

        return ResponseEntity
            .status(ErrorType.INTERNAL_ERROR.status)
            .body(ErrorResponse(ErrorType.INTERNAL_ERROR))
    }
}
