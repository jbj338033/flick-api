package com.flick.support.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,
    val accessExpiration: Long = 1800000,
    val refreshExpiration: Long = 604800000,
)
