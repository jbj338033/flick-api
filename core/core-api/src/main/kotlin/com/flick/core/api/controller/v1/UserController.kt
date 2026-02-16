package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.response.UserResponse
import com.flick.core.api.controller.v1.response.toResponse
import com.flick.core.domain.user.UserService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
) {
    @GetMapping("/me")
    fun getMe(
        @AuthenticationPrincipal userId: UUID,
    ): UserResponse = userService.getUser(userId).toResponse()
}
