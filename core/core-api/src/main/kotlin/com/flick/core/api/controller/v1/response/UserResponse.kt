package com.flick.core.api.controller.v1.response

import com.flick.core.enums.UserRole
import com.flick.storage.db.core.entity.User
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val role: UserRole,
    val balance: Int,
    val grade: Int?,
    val room: Int?,
    val number: Int?,
    val isGrantClaimed: Boolean,
)

fun User.toResponse() =
    UserResponse(
        id = id,
        name = name,
        email = email,
        role = role,
        balance = balance,
        grade = grade,
        room = room,
        number = number,
        isGrantClaimed = isGrantClaimed,
    )
