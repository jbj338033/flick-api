package com.flick.core.domain.payment

import java.util.UUID

data class PaymentItemInput(
    val productId: UUID,
    val quantity: Int,
    val options: List<SelectedOptionInput> = emptyList(),
)

data class SelectedOptionInput(
    val optionId: UUID,
    val quantity: Int = 1,
)
