package com.flick.core.api.controller.v1.response

import com.flick.storage.db.core.entity.Booth
import java.time.LocalDateTime
import java.util.UUID

data class BoothResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val createdAt: LocalDateTime,
)

data class BoothOwnerResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val pairingCode: String?,
    val createdAt: LocalDateTime,
)

fun Booth.toResponse() =
    BoothResponse(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
    )

fun Booth.toOwnerResponse() =
    BoothOwnerResponse(
        id = id,
        name = name,
        description = description,
        pairingCode = pairingCode,
        createdAt = createdAt,
    )
