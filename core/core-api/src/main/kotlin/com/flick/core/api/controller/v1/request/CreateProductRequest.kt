package com.flick.core.api.controller.v1.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class CreateProductRequest(
    @field:NotBlank val name: String,
    @field:Min(1) val price: Int,
    @field:Min(1) val stock: Int? = null,
    @field:Valid val optionGroups: List<CreateOptionGroupRequest> = emptyList(),
)
