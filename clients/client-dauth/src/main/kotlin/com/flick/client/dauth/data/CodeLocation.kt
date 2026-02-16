package com.flick.client.dauth.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CodeLocation(
    val location: String,
)
