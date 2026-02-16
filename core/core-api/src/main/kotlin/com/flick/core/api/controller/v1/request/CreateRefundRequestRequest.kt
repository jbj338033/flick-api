package com.flick.core.api.controller.v1.request

import com.flick.core.enums.Bank
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateRefundRequestRequest(
    @field:NotNull val bank: Bank,
    @field:NotBlank val accountNumber: String,
)
