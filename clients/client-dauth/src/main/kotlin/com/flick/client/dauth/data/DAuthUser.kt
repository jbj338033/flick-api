package com.flick.client.dauth.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DAuthUser(
    val uniqueId: String,
    val grade: Int?,
    val room: Int?,
    val number: Int?,
    val name: String,
    val profileImage: String?,
    val role: String,
    val email: String,
)
