package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.UpdateOptionRequest
import com.flick.core.api.controller.v1.response.ProductOptionResponse
import com.flick.core.domain.product.ProductOptionService
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
@RequestMapping("/api/v1/options")
class OptionManageController(
    private val productOptionService: ProductOptionService,
) {
    @PatchMapping("/{optionId}")
    fun updateOption(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable optionId: UUID,
        @Valid @RequestBody request: UpdateOptionRequest,
    ): ProductOptionResponse =
        productOptionService.updateOption(
            userId = userId,
            optionId = optionId,
            name = request.name,
            price = request.price,
            isQuantitySelectable = request.isQuantitySelectable,
            sortOrder = request.sortOrder,
        )

    @DeleteMapping("/{optionId}")
    fun deleteOption(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable optionId: UUID,
    ) {
        productOptionService.deleteOption(userId, optionId)
    }
}
