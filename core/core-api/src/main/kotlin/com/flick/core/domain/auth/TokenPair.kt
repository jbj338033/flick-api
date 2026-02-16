package com.flick.core.domain.auth

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)
