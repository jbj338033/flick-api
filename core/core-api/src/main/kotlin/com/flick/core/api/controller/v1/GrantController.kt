package com.flick.core.api.controller.v1

import com.flick.core.domain.grant.GrantService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/grant")
class GrantController(
    private val grantService: GrantService,
) {
    @PostMapping
    fun claimGrant(
        @AuthenticationPrincipal userId: UUID,
    ) {
        grantService.claimGrant(userId)
    }
}
