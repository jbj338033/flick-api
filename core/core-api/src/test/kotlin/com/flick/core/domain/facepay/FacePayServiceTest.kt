package com.flick.core.domain.facepay

import com.flick.core.enums.FaceRecognitionStatus
import com.flick.core.enums.UserRole
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.FaceEmbedding
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.FaceEmbeddingRepository
import com.flick.storage.db.core.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional
import java.util.UUID

class FacePayServiceTest :
    FunSpec({
        val faceEmbeddingRepository = mockk<FaceEmbeddingRepository>(relaxed = true)
        val userRepository = mockk<UserRepository>()
        val service = FacePayService(faceEmbeddingRepository, userRepository)

        val user1 = User(dauthId = "u1", name = "홍길동", email = "h@dsm.hs.kr", role = UserRole.STUDENT)
        val user2 = User(dauthId = "u2", name = "김철수", email = "k@dsm.hs.kr", role = UserRole.STUDENT)

        fun validEmbedding(): FloatArray = FloatArray(128) { 0.1f * it }

        fun nearIdenticalEmbedding(
            base: FloatArray,
            noise: Float = 0.001f,
        ): FloatArray = FloatArray(base.size) { base[it] + noise }

        test("register 정상") {
            val embedding = validEmbedding()
            every { userRepository.findById(user1.id) } returns Optional.of(user1)
            every { faceEmbeddingRepository.saveAll(any<List<FaceEmbedding>>()) } answers { firstArg() }

            service.register(user1.id, listOf(embedding))

            verify { faceEmbeddingRepository.deleteByUserId(user1.id) }
            verify { faceEmbeddingRepository.saveAll(any<List<FaceEmbedding>>()) }
        }

        test("register 잘못된 임베딩 차원 → FACE_EMBEDDING_INVALID") {
            val badEmbedding = FloatArray(64) { 1.0f }

            shouldThrow<CoreException> {
                service.register(user1.id, listOf(badEmbedding))
            }.type shouldBe ErrorType.FACE_EMBEDDING_INVALID
        }

        test("register USER_NOT_FOUND") {
            val id = UUID.randomUUID()
            val embedding = validEmbedding()
            every { userRepository.findById(id) } returns Optional.empty()

            shouldThrow<CoreException> {
                service.register(id, listOf(embedding))
            }.type shouldBe ErrorType.USER_NOT_FOUND
        }

        test("recognize MATCHED (score>=0.95, gap>=0.1)") {
            val queryEmbedding = validEmbedding()
            val storedEmbedding = nearIdenticalEmbedding(queryEmbedding)
            val fe = FaceEmbedding(user = user1, embedding = EmbeddingUtils.toByteArray(storedEmbedding))

            every { faceEmbeddingRepository.findAllWithUser() } returns listOf(fe)

            val result = service.recognize(queryEmbedding)
            result.status shouldBe FaceRecognitionStatus.MATCHED
            result.candidates.size shouldBe 1
            result.candidates[0].userId shouldBe user1.id
        }

        test("recognize NO_MATCH (빈 임베딩)") {
            every { faceEmbeddingRepository.findAllWithUser() } returns emptyList()

            val result = service.recognize(validEmbedding())
            result.status shouldBe FaceRecognitionStatus.NO_MATCH
            result.candidates shouldBe emptyList()
        }

        test("recognize FACE_EMBEDDING_INVALID") {
            shouldThrow<CoreException> {
                service.recognize(FloatArray(64) { 1.0f })
            }.type shouldBe ErrorType.FACE_EMBEDDING_INVALID
        }

        test("delete 정상") {
            service.delete(user1.id)
            verify { faceEmbeddingRepository.deleteByUserId(user1.id) }
        }
    })
