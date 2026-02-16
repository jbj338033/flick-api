package com.flick.core.api.controller.v1.response

data class BoothDetailResponse(
    val booth: BoothOwnerResponse,
    val products: List<ProductResponse>,
    val orders: List<OrderResponse>,
)
