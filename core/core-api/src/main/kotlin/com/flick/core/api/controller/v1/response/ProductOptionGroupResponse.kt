package com.flick.core.api.controller.v1.response

import java.util.UUID

data class ProductOptionGroupResponse(
    val id: UUID,
    val name: String,
    val isRequired: Boolean,
    val maxSelections: Int,
    val sortOrder: Int,
    val options: List<ProductOptionResponse>,
)

data class ProductOptionResponse(
    val id: UUID,
    val name: String,
    val price: Int,
    val isQuantitySelectable: Boolean,
    val sortOrder: Int,
)
