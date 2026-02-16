package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.CreateOptionRequest
import com.flick.core.api.controller.v1.request.UpdateOptionGroupRequest
import com.flick.core.api.controller.v1.response.ProductOptionGroupResponse
import com.flick.core.api.controller.v1.response.ProductOptionResponse
import com.flick.core.domain.product.ProductOptionService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/option-groups")
class OptionGroupManageController(
    private val productOptionService: ProductOptionService,
) {
    @PatchMapping("/{groupId}")
    fun updateOptionGroup(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable groupId: UUID,
        @Valid @RequestBody request: UpdateOptionGroupRequest,
    ): ProductOptionGroupResponse =
        productOptionService.updateOptionGroup(
            userId = userId,
            groupId = groupId,
            name = request.name,
            isRequired = request.isRequired,
            maxSelections = request.maxSelections,
            sortOrder = request.sortOrder,
        )

    @DeleteMapping("/{groupId}")
    fun deleteOptionGroup(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable groupId: UUID,
    ) {
        productOptionService.deleteOptionGroup(userId, groupId)
    }

    @PostMapping("/{groupId}/options")
    fun createOption(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable groupId: UUID,
        @Valid @RequestBody request: CreateOptionRequest,
    ): ProductOptionResponse =
        productOptionService.createOption(
            userId = userId,
            groupId = groupId,
            name = request.name,
            price = request.price,
            isQuantitySelectable = request.isQuantitySelectable,
            sortOrder = request.sortOrder,
        )
}
