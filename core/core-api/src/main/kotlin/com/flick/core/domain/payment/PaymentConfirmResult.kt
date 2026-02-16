package com.flick.core.domain.payment

import java.util.UUID

data class PaymentConfirmResult(
    val orderId: UUID,
    val orderNumber: Int,
    val totalAmount: Int,
    val balanceAfter: Int,
)
