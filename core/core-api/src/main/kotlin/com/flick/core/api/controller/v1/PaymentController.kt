package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.CreatePaymentRequest
import com.flick.core.api.controller.v1.request.PaymentItem
import com.flick.core.api.controller.v1.response.BoothRef
import com.flick.core.api.controller.v1.response.PaymentConfirmResponse
import com.flick.core.api.controller.v1.response.PaymentRequestDetailResponse
import com.flick.core.api.controller.v1.response.PaymentRequestItemResponse
import com.flick.core.api.controller.v1.response.PaymentRequestResponse
import com.flick.core.api.controller.v1.response.ProductRef
import com.flick.core.domain.payment.PaymentItemInput
import com.flick.core.domain.payment.PaymentService
import com.flick.core.domain.payment.SelectedOptionInput
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/payment-requests")
class PaymentController(
    private val paymentService: PaymentService,
) {
    @PostMapping
    fun createPaymentRequest(
        @Valid @RequestBody request: CreatePaymentRequest,
    ): PaymentRequestResponse {
        val result =
            paymentService.createPaymentRequest(
                com.flick.core.domain.payment.CreatePaymentRequest(
                    boothId = request.boothId,
                    items = request.items.map { it.toInput() },
                ),
            )
        return PaymentRequestResponse(
            orderId = result.orderId,
            code = result.code,
            orderNumber = result.orderNumber,
            totalAmount = result.totalAmount,
        )
    }

    @GetMapping("/{code}")
    fun getPaymentRequest(
        @PathVariable code: String,
    ): PaymentRequestDetailResponse {
        val result = paymentService.getPaymentRequestByCode(code)
        return PaymentRequestDetailResponse(
            orderId = result.orderId,
            code = result.code,
            orderNumber = result.orderNumber,
            totalAmount = result.totalAmount,
            booth = BoothRef(id = result.boothId, name = result.boothName),
            confirmed = result.confirmed,
            expired = result.expired,
            expiresAt = result.expiresAt,
            items =
                result.items.map {
                    PaymentRequestItemResponse(
                        product = ProductRef(id = it.productId, name = it.productName),
                        quantity = it.quantity,
                        price = it.price,
                    )
                },
        )
    }

    @PatchMapping("/{code}")
    fun confirmPayment(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable code: String,
    ): PaymentConfirmResponse {
        val result = paymentService.confirmPayment(code = code, userId = userId)
        return PaymentConfirmResponse(
            orderId = result.orderId,
            orderNumber = result.orderNumber,
            totalAmount = result.totalAmount,
            balanceAfter = result.balanceAfter,
        )
    }

    private fun PaymentItem.toInput() =
        PaymentItemInput(
            productId = productId,
            quantity = quantity,
            options = options.map { SelectedOptionInput(it.optionId, it.quantity) },
        )
}
