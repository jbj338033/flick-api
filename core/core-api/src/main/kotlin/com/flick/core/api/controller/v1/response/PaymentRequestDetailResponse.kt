package com.flick.core.api.controller.v1.response

import java.time.LocalDateTime
import java.util.UUID

data class PaymentRequestDetailResponse(
    val orderId: UUID,
    val code: String,
    val orderNumber: Int,
    val totalAmount: Int,
    val booth: BoothRef,
    val confirmed: Boolean,
    val expired: Boolean,
    val expiresAt: LocalDateTime,
    val items: List<PaymentRequestItemResponse>,
)

data class PaymentRequestItemResponse(
    val product: ProductRef,
    val quantity: Int,
    val price: Int,
)
