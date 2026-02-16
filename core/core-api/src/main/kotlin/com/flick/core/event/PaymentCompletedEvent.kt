package com.flick.core.event

import java.util.UUID

data class PaymentCompletedEvent(
    val orderId: UUID,
    val userId: UUID,
    val boothId: UUID,
    val totalAmount: Int,
    val orderNumber: Int,
)
