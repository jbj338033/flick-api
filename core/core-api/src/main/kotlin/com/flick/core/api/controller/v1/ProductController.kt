package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.CreateProductRequest
import com.flick.core.api.controller.v1.response.ProductResponse
import com.flick.core.api.controller.v1.response.toResponse
import com.flick.core.domain.booth.BoothService
import com.flick.core.domain.product.OptionGroupInput
import com.flick.core.domain.product.OptionInput
import com.flick.core.domain.product.ProductOptionService
import com.flick.core.domain.product.ProductService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/booths/{boothId}/products")
class ProductController(
    private val productService: ProductService,
    private val productOptionService: ProductOptionService,
    private val boothService: BoothService,
) {
    @GetMapping
    fun getProducts(
        @PathVariable boothId: UUID,
    ): List<ProductResponse> {
        val products = productService.getProductsByBooth(boothId)
        val optionsByProductId = productOptionService.getOptionGroupResponsesByProductIds(products.map { it.id })
        return products.map { it.toResponse(optionsByProductId[it.id] ?: emptyList()) }
    }

    @PostMapping
    fun createProduct(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable boothId: UUID,
        @Valid @RequestBody request: CreateProductRequest,
    ): ProductResponse {
        boothService.verifyOwner(boothId, userId)
        val optionGroups =
            request.optionGroups.map { group ->
                OptionGroupInput(
                    name = group.name,
                    isRequired = group.isRequired,
                    maxSelections = group.maxSelections,
                    sortOrder = group.sortOrder,
                    options =
                        group.options.map {
                            OptionInput(
                                name = it.name,
                                price = it.price,
                                isQuantitySelectable = it.isQuantitySelectable,
                                sortOrder = it.sortOrder,
                            )
                        },
                )
            }
        val product =
            productService.createProduct(
                name = request.name,
                price = request.price,
                stock = request.stock,
                boothId = boothId,
                optionGroups = optionGroups,
            )
        val responseOptionGroups = productOptionService.getOptionGroupsByProduct(product.id)
        return product.toResponse(responseOptionGroups)
    }
}
