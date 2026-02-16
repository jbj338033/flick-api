package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.LoginRequest
import com.flick.core.api.controller.v1.request.RefreshRequest
import com.flick.core.api.controller.v1.response.TokenResponse
import com.flick.core.domain.auth.AuthService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): TokenResponse {
        val tokenPair = authService.login(request.id, request.password)
        return TokenResponse(tokenPair.accessToken, tokenPair.refreshToken)
    }

    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshRequest,
    ): TokenResponse {
        val tokenPair = authService.refresh(request.refreshToken)
        return TokenResponse(tokenPair.accessToken, tokenPair.refreshToken)
    }
}
