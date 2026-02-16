package com.flick.core.api.controller.v1.response

import java.util.UUID

data class PaymentConfirmResponse(
    val orderId: UUID,
    val orderNumber: Int,
    val totalAmount: Int,
    val balanceAfter: Int,
)
