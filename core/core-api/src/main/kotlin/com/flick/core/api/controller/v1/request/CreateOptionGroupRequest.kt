package com.flick.core.api.controller.v1.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

data class CreateOptionGroupRequest(
    @field:NotBlank val name: String,
    val isRequired: Boolean = false,
    val maxSelections: Int = 1,
    val sortOrder: Int = 0,
    @field:Valid val options: List<CreateOptionRequest> = emptyList(),
)
