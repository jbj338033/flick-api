package com.flick.core.api.controller.v1.request

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class FacePayConfirmRequest(
    @field:NotBlank val code: String,
    val userId: UUID,
)
