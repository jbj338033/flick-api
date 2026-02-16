package com.flick.client.dauth.data

import com.fasterxml.jackson.annotation.JsonProperty

data class DAuthTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("refresh_token") val refreshToken: String? = null,
    @JsonProperty("token_type") val tokenType: String,
    @JsonProperty("expires_in") val expiresIn: String,
)
