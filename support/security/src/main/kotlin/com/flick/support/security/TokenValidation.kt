package com.flick.support.security

sealed interface TokenValidation {
    data object Valid : TokenValidation

    data object Expired : TokenValidation

    data object Invalid : TokenValidation
}
