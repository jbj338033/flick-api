package com.flick.client.dauth

import com.flick.client.dauth.data.DAuthTokenResponse
import com.flick.client.dauth.data.DAuthUser
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("local")
class LocalDAuthClient : DAuthClient {
    private val accounts =
        mapOf(
            "student" to
                DAuthUser(
                    uniqueId = "local-student-001",
                    grade = 2,
                    room = 1,
                    number = 15,
                    name = "테스트학생",
                    profileImage = null,
                    role = "STUDENT",
                    email = "student@dsm.hs.kr",
                ),
            "teacher" to
                DAuthUser(
                    uniqueId = "local-teacher-001",
                    grade = null,
                    room = null,
                    number = null,
                    name = "테스트교사",
                    profileImage = null,
                    role = "TEACHER",
                    email = "teacher@dsm.hs.kr",
                ),
            "admin" to
                DAuthUser(
                    uniqueId = "local-admin-001",
                    grade = null,
                    room = null,
                    number = null,
                    name = "테스트관리자",
                    profileImage = null,
                    role = "ADMIN",
                    email = "admin@dsm.hs.kr",
                ),
        )

    override fun login(
        id: String,
        password: String,
    ): DAuthTokenResponse {
        if (!accounts.containsKey(id)) {
            throw DAuthException("Unknown local account: $id (use: student, teacher, admin)")
        }
        return DAuthTokenResponse(
            accessToken = "local-token-$id",
            refreshToken = null,
            tokenType = "Bearer",
            expiresIn = "3600",
        )
    }

    override fun getUser(accessToken: String): DAuthUser {
        val id = accessToken.removePrefix("local-token-")
        return accounts[id]
            ?: throw DAuthException("Unknown local token: $accessToken")
    }
}
