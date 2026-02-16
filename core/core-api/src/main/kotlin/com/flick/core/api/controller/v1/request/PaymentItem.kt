package com.flick.core.api.controller.v1.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import java.util.UUID

data class PaymentItem(
    val productId: UUID,
    @field:Min(1) val quantity: Int,
    @field:Valid val options: List<SelectedOption> = emptyList(),
)

data class SelectedOption(
    val optionId: UUID,
    @field:Min(1) val quantity: Int = 1,
)
