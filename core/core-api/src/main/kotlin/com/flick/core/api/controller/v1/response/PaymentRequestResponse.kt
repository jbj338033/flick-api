package com.flick.core.api.controller.v1.response

import java.util.UUID

data class PaymentRequestResponse(
    val orderId: UUID,
    val code: String,
    val orderNumber: Int,
    val totalAmount: Int,
)
