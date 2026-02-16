package com.flick.core.api.controller.v1.response

data class KioskPairResponse(
    val token: String,
    val booth: BoothRef,
)
