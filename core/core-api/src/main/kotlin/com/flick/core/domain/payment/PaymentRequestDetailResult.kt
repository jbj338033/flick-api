package com.flick.core.domain.payment

import java.time.LocalDateTime
import java.util.UUID

data class PaymentRequestDetailResult(
    val orderId: UUID,
    val code: String,
    val orderNumber: Int,
    val totalAmount: Int,
    val boothId: UUID,
    val boothName: String,
    val confirmed: Boolean,
    val expired: Boolean,
    val expiresAt: LocalDateTime,
    val items: List<PaymentRequestItemResult>,
)

data class PaymentRequestItemResult(
    val productId: UUID,
    val productName: String,
    val quantity: Int,
    val price: Int,
)
