package com.flick.core.api.controller.v1.request

import jakarta.validation.constraints.Min

data class UpdateProductRequest(
    val name: String? = null,
    @field:Min(1) val price: Int? = null,
    @field:Min(0) val stock: Int? = null,
    @field:Min(1) val purchaseLimit: Int? = null,
    val isSoldOut: Boolean? = null,
)
