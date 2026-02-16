package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.response.OrderResponse
import com.flick.core.domain.order.OrderService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderService: OrderService,
) {
    @GetMapping("/me")
    fun getMyOrders(
        @AuthenticationPrincipal userId: UUID,
    ): List<OrderResponse> = orderService.getOrdersWithItemsByUser(userId)

    @PatchMapping("/{orderId}/cancel")
    fun cancelOrder(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable orderId: UUID,
    ) {
        orderService.cancelOrder(orderId = orderId, userId = userId)
    }
}
