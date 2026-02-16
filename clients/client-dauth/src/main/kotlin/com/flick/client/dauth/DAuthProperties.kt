package com.flick.client.dauth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "dauth")
data class DAuthProperties(
    val clientId: String,
    val clientSecret: String,
    val redirectUrl: String,
)
