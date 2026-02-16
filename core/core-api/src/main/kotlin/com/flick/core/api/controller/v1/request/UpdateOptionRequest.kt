package com.flick.core.api.controller.v1.request

data class UpdateOptionRequest(
    val name: String? = null,
    val price: Int? = null,
    val isQuantitySelectable: Boolean? = null,
    val sortOrder: Int? = null,
)
