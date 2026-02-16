package com.flick.core.domain.payment

import java.util.UUID

data class PaymentRequestResult(
    val orderId: UUID,
    val code: String,
    val orderNumber: Int,
    val totalAmount: Int,
)
