package com.flick.core.api.controller.v1.response

import com.flick.core.enums.NotificationType
import com.flick.storage.db.core.entity.Notification
import java.time.LocalDateTime
import java.util.UUID

data class NotificationResponse(
    val id: UUID,
    val type: NotificationType,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val createdAt: LocalDateTime,
)

fun Notification.toResponse() =
    NotificationResponse(
        id = id,
        type = type,
        title = title,
        body = body,
        isRead = isRead,
        createdAt = createdAt,
    )
