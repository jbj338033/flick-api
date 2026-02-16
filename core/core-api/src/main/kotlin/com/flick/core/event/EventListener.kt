package com.flick.core.event

import com.flick.core.domain.kiosk.KioskSseService
import com.flick.core.domain.notification.NotificationService
import com.flick.core.enums.NotificationType
import com.flick.support.logging.logger
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class EventListener(
    private val notificationService: NotificationService,
    private val kioskSseService: KioskSseService,
) {
    private val log = logger()

    @Async("eventExecutor")
    @EventListener
    fun handlePaymentCompleted(event: PaymentCompletedEvent) {
        log.info { "Payment completed: order=${event.orderId}, booth=${event.boothId}" }

        runCatching {
            notificationService.createNotification(
                type = NotificationType.PAYMENT_COMPLETED,
                title = "결제 완료",
                body = "주문 #${event.orderNumber} - ${event.totalAmount}원 결제가 완료되었습니다.",
                userId = event.userId,
            )
        }.onFailure { log.error(it) { "Failed to create payment notification: order=${event.orderId}" } }

        runCatching {
            kioskSseService.sendToKiosk(
                boothId = event.boothId,
                eventName = "payment_completed",
                data =
                    mapOf(
                        "orderId" to event.orderId.toString(),
                        "orderNumber" to event.orderNumber,
                        "totalAmount" to event.totalAmount,
                    ),
            )
        }.onFailure { log.error(it) { "Failed to send SSE to kiosk: booth=${event.boothId}" } }
    }

    @Async("eventExecutor")
    @EventListener
    fun handlePointCharged(event: PointChargedEvent) {
        log.info { "Point charged: user=${event.userId}, amount=${event.amount}" }

        runCatching {
            notificationService.createNotification(
                type = NotificationType.POINT_CHARGED,
                title = "충전 완료",
                body = "${event.amount}원이 충전되었습니다. 잔액: ${event.balanceAfter}원",
                userId = event.userId,
            )
        }.onFailure { log.error(it) { "Failed to create charge notification: user=${event.userId}" } }
    }

    @Async("eventExecutor")
    @EventListener
    fun handleProductChanged(event: ProductChangedEvent) {
        log.info { "Product changed: booth=${event.boothId}" }

        runCatching {
            kioskSseService.sendToKiosk(
                boothId = event.boothId,
                eventName = "product_changed",
                data = mapOf("boothId" to event.boothId.toString()),
            )
        }.onFailure { log.error(it) { "Failed to send product_changed SSE: booth=${event.boothId}" } }
    }

    @Async("eventExecutor")
    @EventListener
    fun handleOrderCancelled(event: OrderCancelledEvent) {
        log.info { "Order cancelled: order=${event.orderId}, booth=${event.boothId}" }

        runCatching {
            notificationService.createNotification(
                type = NotificationType.ORDER_CANCELLED,
                title = "주문 취소",
                body = "주문 #${event.orderNumber} - ${event.totalAmount}원이 환불되었습니다.",
                userId = event.userId,
            )
        }.onFailure { log.error(it) { "Failed to create cancel notification: order=${event.orderId}" } }

        runCatching {
            kioskSseService.sendToKiosk(
                boothId = event.boothId,
                eventName = "order_cancelled",
                data =
                    mapOf(
                        "orderId" to event.orderId.toString(),
                        "orderNumber" to event.orderNumber,
                        "totalAmount" to event.totalAmount,
                    ),
            )
        }.onFailure { log.error(it) { "Failed to send order_cancelled SSE: booth=${event.boothId}" } }
    }
}
