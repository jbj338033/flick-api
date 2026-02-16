package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.FacePayConfirmRequest
import com.flick.core.api.controller.v1.request.RecognizeFaceRequest
import com.flick.core.api.controller.v1.response.PaymentConfirmResponse
import com.flick.core.api.controller.v1.response.RecognizeResponse
import com.flick.core.domain.facepay.FacePayService
import com.flick.core.domain.payment.PaymentService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/kiosk/face-pay")
class KioskFacePayController(
    private val facePayService: FacePayService,
    private val paymentService: PaymentService,
) {
    @PostMapping("/recognize")
    fun recognize(
        @Valid @RequestBody request: RecognizeFaceRequest,
    ): RecognizeResponse {
        val result = facePayService.recognize(request.embedding.toFloatArray())
        return RecognizeResponse(
            status = result.status,
            candidates =
                result.candidates.map {
                    RecognizeResponse.Candidate(userId = it.userId, name = it.name)
                },
        )
    }

    @PostMapping("/confirm")
    fun confirm(
        @Valid @RequestBody request: FacePayConfirmRequest,
    ): PaymentConfirmResponse {
        val result = paymentService.confirmPayment(code = request.code, userId = request.userId)
        return PaymentConfirmResponse(
            orderId = result.orderId,
            orderNumber = result.orderNumber,
            totalAmount = result.totalAmount,
            balanceAfter = result.balanceAfter,
        )
    }
}
