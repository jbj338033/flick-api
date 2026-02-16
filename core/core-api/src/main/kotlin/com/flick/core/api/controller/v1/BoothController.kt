package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.CreateBoothRequest
import com.flick.core.api.controller.v1.request.UpdateBoothRequest
import com.flick.core.api.controller.v1.response.BoothOwnerResponse
import com.flick.core.api.controller.v1.response.BoothResponse
import com.flick.core.api.controller.v1.response.KioskSessionCountResponse
import com.flick.core.api.controller.v1.response.OrderResponse
import com.flick.core.api.controller.v1.response.toOwnerResponse
import com.flick.core.api.controller.v1.response.toResponse
import com.flick.core.domain.booth.BoothService
import com.flick.core.domain.order.OrderService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/booths")
class BoothController(
    private val boothService: BoothService,
    private val orderService: OrderService,
) {
    @GetMapping
    fun getBooths(): List<BoothResponse> = boothService.getAllBooths().map { it.toResponse() }

    @GetMapping("/{boothId}")
    fun getBooth(
        @PathVariable boothId: UUID,
    ): BoothResponse = boothService.getBooth(boothId).toResponse()

    @GetMapping("/me")
    fun getMyBooth(
        @AuthenticationPrincipal userId: UUID,
    ): BoothOwnerResponse? = boothService.getMyBooth(userId)?.toOwnerResponse()

    @PostMapping
    fun createBooth(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: CreateBoothRequest,
    ): BoothOwnerResponse =
        boothService
            .createBooth(name = request.name, description = request.description, userId = userId)
            .toOwnerResponse()

    @PutMapping("/{boothId}")
    fun updateBooth(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable boothId: UUID,
        @Valid @RequestBody request: UpdateBoothRequest,
    ): BoothOwnerResponse {
        boothService.verifyOwner(boothId, userId)
        return boothService
            .updateBooth(boothId = boothId, name = request.name, description = request.description)
            .toOwnerResponse()
    }

    @PostMapping("/{boothId}/pairing-code")
    fun regeneratePairingCode(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable boothId: UUID,
    ): BoothOwnerResponse {
        boothService.verifyOwner(boothId, userId)
        return boothService.generatePairingCode(boothId).toOwnerResponse()
    }

    @GetMapping("/{boothId}/kiosk-sessions")
    fun getKioskSessionCount(
        @PathVariable boothId: UUID,
    ): KioskSessionCountResponse = KioskSessionCountResponse(boothService.getKioskSessionCount(boothId))

    @GetMapping("/{boothId}/orders")
    fun getBoothOrders(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable boothId: UUID,
    ): List<OrderResponse> {
        boothService.verifyOwner(boothId, userId)
        return orderService.getOrdersWithItemsByBooth(boothId)
    }
}
