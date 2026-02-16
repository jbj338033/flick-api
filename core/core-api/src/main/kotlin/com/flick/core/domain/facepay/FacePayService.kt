package com.flick.core.domain.facepay

import com.flick.core.enums.FaceRecognitionStatus
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.FaceEmbedding
import com.flick.storage.db.core.repository.FaceEmbeddingRepository
import com.flick.storage.db.core.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FacePayService(
    private val faceEmbeddingRepository: FaceEmbeddingRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun register(
        userId: UUID,
        embeddings: List<FloatArray>,
    ) {
        embeddings.forEach {
            if (it.size != EMBEDDING_DIMENSION) throw CoreException(ErrorType.FACE_EMBEDDING_INVALID)
        }

        val user = userRepository.findById(userId).orElseThrow { CoreException(ErrorType.USER_NOT_FOUND) }

        faceEmbeddingRepository.deleteByUserId(userId)
        faceEmbeddingRepository.flush()

        faceEmbeddingRepository.saveAll(
            embeddings.map { floats ->
                FaceEmbedding(user = user, embedding = EmbeddingUtils.toByteArray(floats))
            },
        )
    }

    @Transactional(readOnly = true)
    fun recognize(embedding: FloatArray): RecognitionResult {
        if (embedding.size != EMBEDDING_DIMENSION) throw CoreException(ErrorType.FACE_EMBEDDING_INVALID)

        val allEmbeddings = faceEmbeddingRepository.findAllWithUser()
        if (allEmbeddings.isEmpty()) return RecognitionResult(FaceRecognitionStatus.NO_MATCH, emptyList())

        val userScores =
            allEmbeddings
                .groupBy { it.user.id }
                .map { (userId, faceEmbeddings) ->
                    val maxSimilarity =
                        faceEmbeddings.maxOf { fe ->
                            EmbeddingUtils.cosineSimilarity(embedding, EmbeddingUtils.toFloatArray(fe.embedding))
                        }
                    UserScore(userId, faceEmbeddings.first().user.name, maxSimilarity)
                }.sortedByDescending { it.score }

        val top1 = userScores.first()
        val top2Score = if (userScores.size >= 2) userScores[1].score else 0.0
        val gap = top1.score - top2Score

        return when {
            top1.score >= MATCH_THRESHOLD && gap >= GAP_THRESHOLD ->
                RecognitionResult(
                    FaceRecognitionStatus.MATCHED,
                    listOf(FaceCandidate(top1.userId, top1.name)),
                )

            top1.score >= CANDIDATE_THRESHOLD -> {
                val filtered =
                    userScores
                        .filter { it.score >= CANDIDATE_THRESHOLD }
                        .map { FaceCandidate(it.userId, it.name) }
                if (filtered.size >= 2) {
                    RecognitionResult(FaceRecognitionStatus.CANDIDATES, filtered)
                } else {
                    RecognitionResult(FaceRecognitionStatus.MATCHED, filtered)
                }
            }

            else -> RecognitionResult(FaceRecognitionStatus.NO_MATCH, emptyList())
        }
    }

    @Transactional
    fun delete(userId: UUID) {
        faceEmbeddingRepository.deleteByUserId(userId)
    }

    companion object {
        const val EMBEDDING_DIMENSION = 128
        const val MATCH_THRESHOLD = 0.95
        const val GAP_THRESHOLD = 0.1
        const val CANDIDATE_THRESHOLD = 0.85
    }
}

private data class UserScore(
    val userId: UUID,
    val name: String,
    val score: Double,
)
