package com.flick.core.domain.payment

import com.flick.storage.db.core.repository.OrderRepository
import com.flick.storage.db.core.repository.PaymentRequestRepository
import com.flick.support.logging.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class PaymentRequestCleanupScheduler(
    private val paymentRequestRepository: PaymentRequestRepository,
    private val orderRepository: OrderRepository,
    private val paymentCodeService: PaymentCodeService,
) {
    private val log = logger()

    @Scheduled(fixedDelay = 60000)
    @Transactional
    fun cleanupExpiredRequests() {
        val now = LocalDateTime.now()
        val expiredCodes =
            paymentRequestRepository
                .findAllByIsConfirmedFalseAndExpiresAtBefore(now)
                .map { it.code }

        if (expiredCodes.isEmpty()) return

        val updatedCount = orderRepository.cancelExpiredPaymentOrders(now)
        expiredCodes.forEach { paymentCodeService.invalidateCode(it) }

        log.info { "Cleaned up $updatedCount expired payment requests" }
    }
}
