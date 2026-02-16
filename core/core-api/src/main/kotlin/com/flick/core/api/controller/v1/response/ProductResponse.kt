package com.flick.core.api.controller.v1.response

import com.flick.storage.db.core.entity.Product
import java.util.UUID

data class ProductResponse(
    val id: UUID,
    val name: String,
    val price: Int,
    val stock: Int?,
    val isSoldOut: Boolean,
    val purchaseLimit: Int?,
    val booth: BoothRef,
    val optionGroups: List<ProductOptionGroupResponse> = emptyList(),
)

fun Product.toResponse(optionGroups: List<ProductOptionGroupResponse> = emptyList()) =
    ProductResponse(
        id = id,
        name = name,
        price = price,
        stock = stock,
        isSoldOut = isSoldOut,
        purchaseLimit = purchaseLimit,
        booth = BoothRef(id = booth.id, name = booth.name),
        optionGroups = optionGroups,
    )
