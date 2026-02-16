package com.flick.core.domain.order

import java.util.UUID

data class OrderItemInput(
    val productId: UUID,
    val quantity: Int,
    val price: Int,
    val selectedOptions: List<SelectedOptionSnapshot> = emptyList(),
)

data class SelectedOptionSnapshot(
    val groupName: String,
    val name: String,
    val price: Int,
    val quantity: Int,
)
