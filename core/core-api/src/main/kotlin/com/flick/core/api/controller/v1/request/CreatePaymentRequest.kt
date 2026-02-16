package com.flick.core.api.controller.v1.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import java.util.UUID

data class CreatePaymentRequest(
    val boothId: UUID,
    @field:NotEmpty @field:Valid val items: List<PaymentItem>,
)
