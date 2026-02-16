package com.flick.core.api.controller.v1.request

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class RegisterFaceRequest(
    @field:NotEmpty @field:Size(max = 10) val embeddings: List<List<Float>>,
)
