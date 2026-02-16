package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.ChargeRequest
import com.flick.core.api.controller.v1.response.BoothDetailResponse
import com.flick.core.api.controller.v1.response.OrderResponse
import com.flick.core.api.controller.v1.response.UserResponse
import com.flick.core.api.controller.v1.response.toOwnerResponse
import com.flick.core.api.controller.v1.response.toResponse
import com.flick.core.domain.admin.AdminService
import com.flick.core.domain.admin.DashboardStats
import com.flick.core.domain.admin.SettlementExportService
import com.flick.core.domain.booth.BoothService
import com.flick.core.domain.charge.ChargeService
import com.flick.core.domain.order.OrderService
import com.flick.core.domain.product.ProductService
import com.flick.core.domain.user.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val adminService: AdminService,
    private val userService: UserService,
    private val boothService: BoothService,
    private val orderService: OrderService,
    private val productService: ProductService,
    private val settlementExportService: SettlementExportService,
    private val chargeService: ChargeService,
) {
    @GetMapping("/stats")
    fun getStats(): DashboardStats = adminService.getDashboardStats()

    @GetMapping("/users")
    fun getAllUsers(): List<UserResponse> = userService.getAllUsers().map { it.toResponse() }

    @GetMapping("/users/search")
    fun searchUsers(
        @RequestParam q: String,
    ): List<UserResponse> = userService.searchUsers(q).map { it.toResponse() }

    @GetMapping("/booths/{boothId}")
    fun getBoothDetail(
        @PathVariable boothId: UUID,
    ): BoothDetailResponse {
        val booth = boothService.getBooth(boothId)
        val products = productService.getProductsByBooth(boothId).map { it.toResponse() }
        val orders = orderService.getOrdersWithItemsByBooth(boothId)
        return BoothDetailResponse(
            booth = booth.toOwnerResponse(),
            products = products,
            orders = orders,
        )
    }

    @GetMapping("/orders")
    fun getAllOrders(): List<OrderResponse> = orderService.getAllOrdersWithItems()

    @GetMapping("/settlement/export")
    fun exportSettlement(): ResponseEntity<ByteArray> {
        val bytes = settlementExportService.exportSettlement()
        return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=settlement.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes)
    }

    @PostMapping("/charge")
    fun charge(
        @Valid @RequestBody request: ChargeRequest,
    ) {
        chargeService.charge(grade = request.grade, room = request.room, number = request.number, amount = request.amount)
    }
}
