package com.flick.core.api.controller.v1.response

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)
