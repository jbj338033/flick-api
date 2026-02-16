package com.flick.core.domain.notification

import com.flick.core.enums.NotificationType
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Notification
import com.flick.storage.db.core.repository.NotificationRepository
import com.flick.storage.db.core.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
) {
    fun getNotifications(userId: UUID): List<Notification> = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId)

    fun getUnreadCount(userId: UUID): Long = notificationRepository.countByUserIdAndIsReadFalse(userId)

    @Transactional
    fun markAsRead(
        userId: UUID,
        notificationId: UUID,
    ) {
        val notification =
            notificationRepository.findByIdAndUserId(notificationId, userId)
                ?: throw CoreException(ErrorType.NOTIFICATION_NOT_FOUND)
        notification.isRead = true
    }

    @Transactional
    fun markAllAsRead(userId: UUID) {
        notificationRepository.markAllAsReadByUserId(userId)
    }

    @Transactional
    fun createNotification(
        userId: UUID,
        type: NotificationType,
        title: String,
        body: String,
    ): Notification {
        val user = userRepository.getReferenceById(userId)
        return notificationRepository.save(
            Notification(type = type, title = title, body = body, user = user),
        )
    }
}
