package com.flick.core.api.controller.v1.request

import jakarta.validation.constraints.NotBlank

data class CreateBoothRequest(
    @field:NotBlank val name: String,
    val description: String? = null,
)
