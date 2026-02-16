package com.flick.core.api.controller.v1.request

import jakarta.validation.constraints.NotBlank

data class CreateOptionRequest(
    @field:NotBlank val name: String,
    val price: Int,
    val isQuantitySelectable: Boolean = false,
    val sortOrder: Int = 0,
)
