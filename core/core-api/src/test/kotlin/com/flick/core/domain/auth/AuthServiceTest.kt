package com.flick.core.domain.auth

import com.flick.client.dauth.DAuthClient
import com.flick.client.dauth.data.DAuthTokenResponse
import com.flick.client.dauth.data.DAuthUser
import com.flick.core.enums.UserRole
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.UserRepository
import com.flick.support.security.JwtProvider
import com.flick.support.security.TokenValidation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID

class AuthServiceTest :
    FunSpec({
        val dAuthClient = mockk<DAuthClient>()
        val userRepository = mockk<UserRepository>()
        val jwtProvider = mockk<JwtProvider>()
        val service = AuthService(dAuthClient, userRepository, jwtProvider)

        val dauthToken = DAuthTokenResponse(accessToken = "dauth-access", tokenType = "Bearer", expiresIn = "3600")
        val dauthUser =
            DAuthUser(
                uniqueId = "unique-1",
                grade = 1,
                room = 2,
                number = 3,
                name = "홍길동",
                profileImage = null,
                role = "STUDENT",
                email = "hong@dsm.hs.kr",
            )

        test("login 신규 사용자 → 사용자 생성 + 토큰 반환") {
            every { dAuthClient.login("id", "pw") } returns dauthToken
            every { dAuthClient.getUser("dauth-access") } returns dauthUser
            every { userRepository.findByDauthId("unique-1") } returns null
            every { userRepository.save(any<User>()) } answers { firstArg() }
            every { jwtProvider.createAccessToken(any(), any(), any()) } returns "access"
            every { jwtProvider.createRefreshToken(any(), any()) } returns "refresh"

            val result = service.login("id", "pw")
            result.accessToken shouldBe "access"
            result.refreshToken shouldBe "refresh"
            verify { userRepository.save(any<User>()) }
        }

        test("login 기존 사용자 → 정보 업데이트") {
            val existingUser = User(dauthId = "unique-1", name = "옛이름", email = "old@dsm.hs.kr", role = UserRole.STUDENT)
            every { dAuthClient.login("id", "pw") } returns dauthToken
            every { dAuthClient.getUser("dauth-access") } returns dauthUser
            every { userRepository.findByDauthId("unique-1") } returns existingUser
            every { jwtProvider.createAccessToken(any(), any(), any()) } returns "access"
            every { jwtProvider.createRefreshToken(any(), any()) } returns "refresh"

            service.login("id", "pw")
            existingUser.name shouldBe "홍길동"
            existingUser.email shouldBe "hong@dsm.hs.kr"
        }

        test("login DAuth 로그인 실패 → DAUTH_LOGIN_FAILED") {
            every { dAuthClient.login("id", "pw") } throws RuntimeException("connection failed")

            val exception = shouldThrow<CoreException> { service.login("id", "pw") }
            exception.type shouldBe ErrorType.DAUTH_LOGIN_FAILED
        }

        test("login DAuth 사용자 정보 조회 실패 → DAUTH_USER_FETCH_FAILED") {
            every { dAuthClient.login("id", "pw") } returns dauthToken
            every { dAuthClient.getUser("dauth-access") } throws RuntimeException("fetch failed")

            val exception = shouldThrow<CoreException> { service.login("id", "pw") }
            exception.type shouldBe ErrorType.DAUTH_USER_FETCH_FAILED
        }

        test("login 역할 매핑 TEACHER") {
            val teacherUser = dauthUser.copy(role = "TEACHER")
            every { dAuthClient.login("id", "pw") } returns dauthToken
            every { dAuthClient.getUser("dauth-access") } returns teacherUser
            every { userRepository.findByDauthId("unique-1") } returns null
            every { userRepository.save(any<User>()) } answers { firstArg() }
            every { jwtProvider.createAccessToken(any(), eq("TEACHER"), any()) } returns "access"
            every { jwtProvider.createRefreshToken(any(), any()) } returns "refresh"

            service.login("id", "pw")
            verify { jwtProvider.createAccessToken(any(), eq("TEACHER"), any()) }
        }

        test("refresh 정상") {
            val userId = UUID.randomUUID()
            val user = User(dauthId = "u1", name = "홍길동", email = "h@dsm.hs.kr", role = UserRole.STUDENT)
            every { jwtProvider.validateToken("refresh-token") } returns TokenValidation.Valid
            every { jwtProvider.getUserId("refresh-token") } returns userId
            every { userRepository.findByIdOrNull(userId) } returns user
            every { jwtProvider.createAccessToken(any(), any(), any()) } returns "new-access"
            every { jwtProvider.createRefreshToken(any(), any()) } returns "new-refresh"

            val result = service.refresh("refresh-token")
            result.accessToken shouldBe "new-access"
            result.refreshToken shouldBe "new-refresh"
        }

        test("refresh 만료된 토큰 → TOKEN_EXPIRED") {
            every { jwtProvider.validateToken("expired-token") } returns TokenValidation.Expired

            val exception = shouldThrow<CoreException> { service.refresh("expired-token") }
            exception.type shouldBe ErrorType.TOKEN_EXPIRED
        }

        test("refresh 잘못된 토큰 → INVALID_TOKEN") {
            every { jwtProvider.validateToken("bad-token") } returns TokenValidation.Invalid

            val exception = shouldThrow<CoreException> { service.refresh("bad-token") }
            exception.type shouldBe ErrorType.INVALID_TOKEN
        }
    })
