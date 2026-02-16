package com.flick.core.api.controller.v1.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class ChargeRequest(
    @field:Min(1) @field:Max(3) val grade: Int,
    @field:Min(1) @field:Max(4) val room: Int,
    @field:Min(1) @field:Max(20) val number: Int,
    @field:Min(1) @field:Max(500000) val amount: Int,
)
