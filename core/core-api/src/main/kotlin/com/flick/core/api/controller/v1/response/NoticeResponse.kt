package com.flick.core.api.controller.v1.response

import com.flick.storage.db.core.entity.Notice
import java.time.LocalDateTime
import java.util.UUID

data class NoticeResponse(
    val id: UUID,
    val title: String,
    val content: String,
    val createdAt: LocalDateTime,
)

fun Notice.toResponse() =
    NoticeResponse(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt,
    )
