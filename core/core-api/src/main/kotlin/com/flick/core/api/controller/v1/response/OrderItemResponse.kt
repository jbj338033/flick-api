package com.flick.core.api.controller.v1.response

import java.util.UUID

data class OrderItemResponse(
    val product: ProductRef,
    val quantity: Int,
    val price: Int,
    val options: List<OrderItemOptionGroupResponse> = emptyList(),
)

data class ProductRef(
    val id: UUID,
    val name: String,
)
