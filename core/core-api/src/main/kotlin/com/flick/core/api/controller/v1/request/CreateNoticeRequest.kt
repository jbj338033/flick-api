package com.flick.core.api.controller.v1.request

import jakarta.validation.constraints.NotBlank

data class CreateNoticeRequest(
    @field:NotBlank val title: String,
    @field:NotBlank val content: String,
)
