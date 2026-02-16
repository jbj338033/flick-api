package com.flick.core.api.controller.v1.response

data class OrderItemOptionGroupResponse(
    val groupName: String,
    val selections: List<OrderItemOptionResponse>,
)

data class OrderItemOptionResponse(
    val name: String,
    val price: Int,
    val quantity: Int,
)
