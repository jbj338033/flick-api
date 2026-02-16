package com.flick.core.domain.payment

import com.flick.storage.db.core.entity.Booth
import com.flick.storage.db.core.entity.Order
import com.flick.storage.db.core.entity.PaymentRequest
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.OrderRepository
import com.flick.storage.db.core.repository.PaymentRequestRepository
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class PaymentRequestCleanupSchedulerTest :
    FunSpec({
        val paymentRequestRepository = mockk<PaymentRequestRepository>()
        val orderRepository = mockk<OrderRepository>()
        val paymentCodeService = mockk<PaymentCodeService>(relaxed = true)
        val scheduler = PaymentRequestCleanupScheduler(paymentRequestRepository, orderRepository, paymentCodeService)

        beforeEach { clearMocks(paymentRequestRepository, orderRepository, paymentCodeService) }

        test("만료된 요청 정리") {
            val user = User(dauthId = "o", name = "오너", email = "o@dsm.hs.kr")
            val booth = Booth(name = "부스", user = user)
            val order = Order(orderNumber = 1, totalAmount = 3000, booth = booth)
            val expiredPr = PaymentRequest(code = "111111", order = order, expiresAt = LocalDateTime.now().minusMinutes(5))

            every { paymentRequestRepository.findAllByIsConfirmedFalseAndExpiresAtBefore(any()) } returns listOf(expiredPr)
            every { orderRepository.cancelExpiredPaymentOrders(any()) } returns 1

            scheduler.cleanupExpiredRequests()

            verify { orderRepository.cancelExpiredPaymentOrders(any()) }
            verify { paymentCodeService.invalidateCode("111111") }
        }

        test("만료 없으면 아무 동작 없음") {
            every { paymentRequestRepository.findAllByIsConfirmedFalseAndExpiresAtBefore(any()) } returns emptyList()

            scheduler.cleanupExpiredRequests()

            verify(exactly = 0) { orderRepository.cancelExpiredPaymentOrders(any()) }
            verify(exactly = 0) { paymentCodeService.invalidateCode(any()) }
        }
    })
