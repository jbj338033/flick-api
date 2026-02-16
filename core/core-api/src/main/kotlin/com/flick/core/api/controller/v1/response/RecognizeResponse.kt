package com.flick.core.api.controller.v1.response

import com.flick.core.enums.FaceRecognitionStatus
import java.util.UUID

data class RecognizeResponse(
    val status: FaceRecognitionStatus,
    val candidates: List<Candidate>,
) {
    data class Candidate(
        val userId: UUID,
        val name: String,
    )
}
