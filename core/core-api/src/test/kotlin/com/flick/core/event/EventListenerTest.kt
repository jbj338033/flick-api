package com.flick.core.event

import com.flick.core.domain.kiosk.KioskSseService
import com.flick.core.domain.notification.NotificationService
import com.flick.core.enums.NotificationType
import com.flick.storage.db.core.entity.Notification
import com.flick.storage.db.core.entity.User
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class EventListenerTest :
    FunSpec({
        val notificationService = mockk<NotificationService>()
        val kioskSseService = mockk<KioskSseService>(relaxed = true)
        val listener = EventListener(notificationService, kioskSseService)

        val userId = UUID.randomUUID()
        val boothId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val mockUser = mockk<User>()
        val mockNotification = mockk<Notification>()

        test("handlePaymentCompleted → 알림 + SSE") {
            every {
                notificationService.createNotification(
                    userId = userId,
                    type = NotificationType.PAYMENT_COMPLETED,
                    title = any(),
                    body = any(),
                )
            } returns mockNotification

            val event = PaymentCompletedEvent(orderId, userId, boothId, 3000, 1)
            listener.handlePaymentCompleted(event)

            verify {
                notificationService.createNotification(
                    userId = userId,
                    type = NotificationType.PAYMENT_COMPLETED,
                    title = any(),
                    body = any(),
                )
            }
            verify { kioskSseService.sendToKiosk(boothId, "payment_completed", any()) }
        }

        test("handlePaymentCompleted → 알림 실패해도 SSE 계속") {
            every {
                notificationService.createNotification(
                    userId = userId,
                    type = NotificationType.PAYMENT_COMPLETED,
                    title = any(),
                    body = any(),
                )
            } throws RuntimeException("DB error")

            val event = PaymentCompletedEvent(orderId, userId, boothId, 3000, 1)
            listener.handlePaymentCompleted(event)

            verify { kioskSseService.sendToKiosk(boothId, "payment_completed", any()) }
        }

        test("handlePointCharged → 알림") {
            every {
                notificationService.createNotification(
                    userId = userId,
                    type = NotificationType.POINT_CHARGED,
                    title = any(),
                    body = any(),
                )
            } returns mockNotification

            val event = PointChargedEvent(userId, 5000, 10000)
            listener.handlePointCharged(event)

            verify {
                notificationService.createNotification(
                    userId = userId,
                    type = NotificationType.POINT_CHARGED,
                    title = any(),
                    body = any(),
                )
            }
        }

        test("handleProductChanged → SSE") {
            val event = ProductChangedEvent(boothId)
            listener.handleProductChanged(event)

            verify { kioskSseService.sendToKiosk(boothId, "product_changed", any()) }
        }

        test("handleOrderCancelled → 알림 + SSE") {
            every {
                notificationService.createNotification(
                    userId = userId,
                    type = NotificationType.ORDER_CANCELLED,
                    title = any(),
                    body = any(),
                )
            } returns mockNotification

            val event = OrderCancelledEvent(orderId, userId, boothId, 3000, 1)
            listener.handleOrderCancelled(event)

            verify {
                notificationService.createNotification(
                    userId = userId,
                    type = NotificationType.ORDER_CANCELLED,
                    title = any(),
                    body = any(),
                )
            }
            verify { kioskSseService.sendToKiosk(boothId, "order_cancelled", any()) }
        }

        test("handleOrderCancelled → SSE 실패해도 예외 전파 없음") {
            every {
                notificationService.createNotification(
                    userId = userId,
                    type = NotificationType.ORDER_CANCELLED,
                    title = any(),
                    body = any(),
                )
            } returns mockNotification
            every { kioskSseService.sendToKiosk(boothId, "order_cancelled", any()) } throws RuntimeException("SSE error")

            val event = OrderCancelledEvent(orderId, userId, boothId, 3000, 1)
            listener.handleOrderCancelled(event)
        }
    })
