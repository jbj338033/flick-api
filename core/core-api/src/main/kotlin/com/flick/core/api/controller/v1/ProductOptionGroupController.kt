package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.CreateOptionGroupRequest
import com.flick.core.api.controller.v1.response.ProductOptionGroupResponse
import com.flick.core.domain.product.OptionGroupInput
import com.flick.core.domain.product.OptionInput
import com.flick.core.domain.product.ProductOptionService
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
@RequestMapping("/api/v1/products/{productId}/option-groups")
class ProductOptionGroupController(
    private val productOptionService: ProductOptionService,
) {
    @GetMapping
    fun getOptionGroups(
        @PathVariable productId: UUID,
    ): List<ProductOptionGroupResponse> = productOptionService.getOptionGroupsByProduct(productId)

    @PostMapping
    fun createOptionGroup(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable productId: UUID,
        @Valid @RequestBody request: CreateOptionGroupRequest,
    ): ProductOptionGroupResponse =
        productOptionService.createOptionGroup(
            userId = userId,
            productId = productId,
            input = request.toInput(),
        )
}

fun CreateOptionGroupRequest.toInput() =
    OptionGroupInput(
        name = name,
        isRequired = isRequired,
        maxSelections = maxSelections,
        sortOrder = sortOrder,
        options =
            options.map {
                OptionInput(
                    name = it.name,
                    price = it.price,
                    isQuantitySelectable = it.isQuantitySelectable,
                    sortOrder = it.sortOrder,
                )
            },
    )
