package com.flick.core.api.controller.v1.request

import jakarta.validation.constraints.NotEmpty

data class RecognizeFaceRequest(
    @field:NotEmpty val embedding: List<Float>,
)
