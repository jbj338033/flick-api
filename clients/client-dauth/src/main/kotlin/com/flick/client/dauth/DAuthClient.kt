package com.flick.client.dauth

import com.flick.client.dauth.data.DAuthTokenResponse
import com.flick.client.dauth.data.DAuthUser

interface DAuthClient {
    fun login(
        id: String,
        password: String,
    ): DAuthTokenResponse

    fun getUser(accessToken: String): DAuthUser
}
