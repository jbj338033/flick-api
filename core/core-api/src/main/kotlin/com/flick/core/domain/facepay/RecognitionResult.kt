package com.flick.core.domain.facepay

import com.flick.core.enums.FaceRecognitionStatus
import java.util.UUID

data class RecognitionResult(
    val status: FaceRecognitionStatus,
    val candidates: List<FaceCandidate>,
) {
    init {
        when (status) {
            FaceRecognitionStatus.MATCHED ->
                require(candidates.size == 1) { "MATCHED requires exactly 1 candidate, got ${candidates.size}" }
            FaceRecognitionStatus.CANDIDATES ->
                require(candidates.size >= 2) { "CANDIDATES requires >= 2 candidates, got ${candidates.size}" }
            FaceRecognitionStatus.NO_MATCH ->
                require(candidates.isEmpty()) { "NO_MATCH requires 0 candidates, got ${candidates.size}" }
        }
    }
}

data class FaceCandidate(
    val userId: UUID,
    val name: String,
)
