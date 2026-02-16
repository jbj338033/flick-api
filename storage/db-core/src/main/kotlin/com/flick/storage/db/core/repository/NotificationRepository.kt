package com.flick.storage.db.core.repository

import com.flick.storage.db.core.entity.Notification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface NotificationRepository : JpaRepository<Notification, UUID> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<Notification>

    fun countByUserIdAndIsReadFalse(userId: UUID): Long

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Notification?

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    fun markAllAsReadByUserId(userId: UUID): Int
}
