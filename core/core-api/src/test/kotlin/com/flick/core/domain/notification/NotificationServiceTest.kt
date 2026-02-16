package com.flick.core.domain.notification

import com.flick.core.enums.NotificationType
import com.flick.core.enums.UserRole
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Notification
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.NotificationRepository
import com.flick.storage.db.core.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class NotificationServiceTest :
    FunSpec({
        val notificationRepository = mockk<NotificationRepository>()
        val userRepository = mockk<UserRepository>()
        val service = NotificationService(notificationRepository, userRepository)

        val user = User(dauthId = "u1", name = "테스터", email = "t@dsm.hs.kr", role = UserRole.STUDENT)
        val notification =
            Notification(
                type = NotificationType.PAYMENT_COMPLETED,
                title = "결제 완료",
                body = "1000원 결제",
                user = user,
            )

        test("getNotifications 정상") {
            every { notificationRepository.findAllByUserIdOrderByCreatedAtDesc(user.id) } returns listOf(notification)

            val result = service.getNotifications(user.id)
            result.size shouldBe 1
            result[0].title shouldBe "결제 완료"
        }

        test("getUnreadCount 정상") {
            every { notificationRepository.countByUserIdAndIsReadFalse(user.id) } returns 5L

            service.getUnreadCount(user.id) shouldBe 5L
        }

        test("markAsRead 정상") {
            every { notificationRepository.findByIdAndUserId(notification.id, user.id) } returns notification

            service.markAsRead(user.id, notification.id)
            notification.isRead shouldBe true
        }

        test("markAsRead 존재하지 않음 → NOTIFICATION_NOT_FOUND") {
            val id = UUID.randomUUID()
            every { notificationRepository.findByIdAndUserId(id, user.id) } returns null

            val exception = shouldThrow<CoreException> { service.markAsRead(user.id, id) }
            exception.type shouldBe ErrorType.NOTIFICATION_NOT_FOUND
        }

        test("markAllAsRead 정상") {
            every { notificationRepository.markAllAsReadByUserId(user.id) } returns 3

            service.markAllAsRead(user.id)
            verify { notificationRepository.markAllAsReadByUserId(user.id) }
        }

        test("createNotification 정상") {
            every { userRepository.getReferenceById(user.id) } returns user
            every { notificationRepository.save(any<Notification>()) } answers { firstArg() }

            val result =
                service.createNotification(
                    userId = user.id,
                    type = NotificationType.POINT_CHARGED,
                    title = "충전",
                    body = "5000원 충전됨",
                )
            result.type shouldBe NotificationType.POINT_CHARGED
            result.title shouldBe "충전"
        }
    })
