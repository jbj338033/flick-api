package com.flick.core.domain.payment

import java.util.UUID

data class CreatePaymentRequest(
    val boothId: UUID,
    val items: List<PaymentItemInput>,
)
