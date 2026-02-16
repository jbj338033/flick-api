package com.flick.core.domain.auth

import com.flick.client.dauth.DAuthClient
import com.flick.core.enums.UserRole
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.UserRepository
import com.flick.support.security.JwtProvider
import com.flick.support.security.TokenValidation
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val dAuthClient: DAuthClient,
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider,
) {
    @Transactional
    fun login(
        id: String,
        password: String,
    ): TokenPair {
        val dauthToken =
            runCatching { dAuthClient.login(id, password) }
                .getOrElse { throw CoreException(ErrorType.DAUTH_LOGIN_FAILED) }

        val dauthUser =
            runCatching { dAuthClient.getUser(dauthToken.accessToken) }
                .getOrElse { throw CoreException(ErrorType.DAUTH_USER_FETCH_FAILED) }

        val role = mapDAuthRole(dauthUser.role)
        val user =
            userRepository.findByDauthId(dauthUser.uniqueId)?.apply {
                name = dauthUser.name
                email = dauthUser.email
                this.role = role
                grade = dauthUser.grade
                room = dauthUser.room
                number = dauthUser.number
            } ?: userRepository.save(
                User(
                    dauthId = dauthUser.uniqueId,
                    name = dauthUser.name,
                    email = dauthUser.email,
                    role = role,
                    grade = dauthUser.grade,
                    room = dauthUser.room,
                    number = dauthUser.number,
                ),
            )

        return createTokenPair(user)
    }

    fun refresh(refreshToken: String): TokenPair {
        when (jwtProvider.validateToken(refreshToken)) {
            TokenValidation.Expired -> throw CoreException(ErrorType.TOKEN_EXPIRED)
            TokenValidation.Invalid -> throw CoreException(ErrorType.INVALID_TOKEN)
            TokenValidation.Valid -> {}
        }

        val userId = jwtProvider.getUserId(refreshToken)
        val user =
            userRepository.findByIdOrNull(userId)
                ?: throw CoreException(ErrorType.USER_NOT_FOUND)

        return createTokenPair(user)
    }

    private fun createTokenPair(user: User): TokenPair =
        TokenPair(
            accessToken = jwtProvider.createAccessToken(user.id, user.role.name, "USER"),
            refreshToken = jwtProvider.createRefreshToken(user.id, "USER"),
        )

    private fun mapDAuthRole(dauthRole: String): UserRole =
        when (dauthRole) {
            "TEACHER" -> UserRole.TEACHER
            "ADMIN" -> UserRole.ADMIN
            else -> UserRole.STUDENT
        }
}
