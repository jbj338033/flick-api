package com.flick.core.api.controller.v1.request

import jakarta.validation.constraints.Pattern

data class KioskPairRequest(
    @field:Pattern(regexp = "\\d{4}") val pairingCode: String,
)
