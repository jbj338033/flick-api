package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.UpdateProductRequest
import com.flick.core.api.controller.v1.response.ProductResponse
import com.flick.core.api.controller.v1.response.toResponse
import com.flick.core.domain.product.ProductOptionService
import com.flick.core.domain.product.ProductService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/products")
class ProductManageController(
    private val productService: ProductService,
    private val productOptionService: ProductOptionService,
) {
    @PatchMapping("/{productId}")
    fun updateProduct(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable productId: UUID,
        @Valid @RequestBody request: UpdateProductRequest,
    ): ProductResponse {
        val product =
            productService.updateProduct(
                userId = userId,
                productId = productId,
                name = request.name,
                price = request.price,
                stock = request.stock,
                purchaseLimit = request.purchaseLimit,
                isSoldOut = request.isSoldOut,
            )
        return product.toResponse(productOptionService.getOptionGroupsByProduct(productId))
    }

    @DeleteMapping("/{productId}")
    fun deleteProduct(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable productId: UUID,
    ) {
        productService.deleteProduct(userId, productId)
    }
}
