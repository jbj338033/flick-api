package com.flick.client.dauth

import com.flick.client.dauth.data.DAuthCodeResponse
import com.flick.client.dauth.data.DAuthTokenResponse
import com.flick.client.dauth.data.DAuthUser
import com.flick.client.dauth.data.DAuthUserResponse
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
@Profile("!local")
class DefaultDAuthClient(
    private val dauthRestClient: RestClient,
    private val dodamRestClient: RestClient,
    private val properties: DAuthProperties,
) : DAuthClient {
    override fun login(
        id: String,
        password: String,
    ): DAuthTokenResponse {
        val codeResponse =
            dauthRestClient
                .post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    mapOf(
                        "id" to id,
                        "pw" to password,
                        "clientId" to properties.clientId,
                        "redirectUrl" to properties.redirectUrl,
                    ),
                ).retrieve()
                .body(DAuthCodeResponse::class.java)
                ?: throw DAuthException("Failed to get auth code from DAuth")

        val code =
            codeResponse.data.location
                .substringAfter("code=")
                .substringBefore("&")

        return dauthRestClient
            .post()
            .uri("/api/token")
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                mapOf(
                    "code" to code,
                    "client_id" to properties.clientId,
                    "client_secret" to properties.clientSecret,
                ),
            ).retrieve()
            .body(DAuthTokenResponse::class.java)
            ?: throw DAuthException("Failed to exchange token from DAuth")
    }

    override fun getUser(accessToken: String): DAuthUser {
        val response =
            dodamRestClient
                .get()
                .uri("/api/user")
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .body(DAuthUserResponse::class.java)
                ?: throw DAuthException("Failed to fetch user from DAuth")
        return response.data
    }
}
