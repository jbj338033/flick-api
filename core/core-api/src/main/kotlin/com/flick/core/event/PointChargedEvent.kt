package com.flick.core.event

import java.util.UUID

data class PointChargedEvent(
    val userId: UUID,
    val amount: Int,
    val balanceAfter: Int,
)
