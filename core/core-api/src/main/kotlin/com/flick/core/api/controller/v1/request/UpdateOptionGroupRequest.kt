package com.flick.core.api.controller.v1.request

data class UpdateOptionGroupRequest(
    val name: String? = null,
    val isRequired: Boolean? = null,
    val maxSelections: Int? = null,
    val sortOrder: Int? = null,
)
