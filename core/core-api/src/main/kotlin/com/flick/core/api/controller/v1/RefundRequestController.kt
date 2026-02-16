package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.CreateRefundRequestRequest
import com.flick.core.api.controller.v1.response.RefundRequestCreatedResponse
import com.flick.core.api.controller.v1.response.RefundRequestResponse
import com.flick.core.api.controller.v1.response.toCreatedResponse
import com.flick.core.api.controller.v1.response.toResponse
import com.flick.core.domain.refund.RefundRequestService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/refund-requests")
class RefundRequestController(
    private val refundRequestService: RefundRequestService,
) {
    @PostMapping
    fun createRefundRequest(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: CreateRefundRequestRequest,
    ): RefundRequestCreatedResponse =
        refundRequestService
            .createRefundRequest(userId = userId, bank = request.bank, accountNumber = request.accountNumber)
            .toCreatedResponse()

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun getRefundRequests(): List<RefundRequestResponse> = refundRequestService.getAllRefundRequests().map { it.toResponse() }
}
