package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.RegisterFaceRequest
import com.flick.core.domain.facepay.FacePayService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/face-pay")
class FacePayController(
    private val facePayService: FacePayService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: RegisterFaceRequest,
    ) {
        facePayService.register(
            userId,
            request.embeddings.map { it.toFloatArray() },
        )
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal userId: UUID,
    ) {
        facePayService.delete(userId)
    }
}
