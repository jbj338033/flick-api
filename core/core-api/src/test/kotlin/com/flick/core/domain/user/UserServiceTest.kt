package com.flick.core.domain.user

import com.flick.core.enums.UserRole
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID

class UserServiceTest :
    FunSpec({
        val userRepository = mockk<UserRepository>()
        val service = UserService(userRepository)

        val user =
            User(
                dauthId = "u1",
                name = "홍길동",
                email = "hong@dsm.hs.kr",
                role = UserRole.STUDENT,
                grade = 1,
                room = 2,
                number = 3,
            )

        test("getUser 정상") {
            every { userRepository.findByIdOrNull(user.id) } returns user

            service.getUser(user.id).name shouldBe "홍길동"
        }

        test("getUser 존재하지 않음 → USER_NOT_FOUND") {
            val id = UUID.randomUUID()
            every { userRepository.findByIdOrNull(id) } returns null

            val exception = shouldThrow<CoreException> { service.getUser(id) }
            exception.type shouldBe ErrorType.USER_NOT_FOUND
        }

        test("getAllUsers 정상") {
            every { userRepository.findAll() } returns listOf(user)

            service.getAllUsers().size shouldBe 1
        }

        test("searchUsers 4자리 학번 검색 → 결과 있음") {
            every { userRepository.findByGradeAndRoomAndNumber(1, 2, 3) } returns user

            val result = service.searchUsers("1203")
            result.size shouldBe 1
            result[0].name shouldBe "홍길동"
        }

        test("searchUsers 4자리 학번 → 결과 없음") {
            every { userRepository.findByGradeAndRoomAndNumber(9, 9, 99) } returns null

            service.searchUsers("9999").shouldBeEmpty()
        }

        test("searchUsers 이름 검색") {
            every { userRepository.findByNameContaining("홍") } returns listOf(user)

            val result = service.searchUsers("홍")
            result.size shouldBe 1
        }
    })
