package com.flick.client.dauth.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DAuthCodeResponse(
    val data: CodeLocation,
)
