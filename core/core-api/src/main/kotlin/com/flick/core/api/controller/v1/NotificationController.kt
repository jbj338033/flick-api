package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.response.NotificationResponse
import com.flick.core.api.controller.v1.response.UnreadCountResponse
import com.flick.core.api.controller.v1.response.toResponse
import com.flick.core.domain.notification.NotificationService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService,
) {
    @GetMapping
    fun getNotifications(
        @AuthenticationPrincipal userId: UUID,
    ): List<NotificationResponse> = notificationService.getNotifications(userId).map { it.toResponse() }

    @GetMapping("/unread-count")
    fun getUnreadCount(
        @AuthenticationPrincipal userId: UUID,
    ): UnreadCountResponse = UnreadCountResponse(notificationService.getUnreadCount(userId))

    @PostMapping("/{notificationId}/read")
    fun markAsRead(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable notificationId: UUID,
    ) {
        notificationService.markAsRead(userId, notificationId)
    }

    @PostMapping("/read-all")
    fun markAllAsRead(
        @AuthenticationPrincipal userId: UUID,
    ) {
        notificationService.markAllAsRead(userId)
    }
}
