package com.flick.core.domain.payment

import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.redis.PaymentCodeRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class PaymentCodeServiceTest :
    FunSpec({
        val paymentCodeRepository = mockk<PaymentCodeRepository>()
        val service = PaymentCodeService(paymentCodeRepository)

        test("generateCode 정상 → 6자리 코드") {
            val orderId = UUID.randomUUID()
            every { paymentCodeRepository.saveIfAbsent(any(), eq(orderId)) } returns true

            val code = service.generateCode(orderId)
            code shouldMatch "\\d{6}"
        }

        test("generateCode 충돌 시 재시도 후 성공") {
            val orderId = UUID.randomUUID()
            every { paymentCodeRepository.saveIfAbsent(any(), eq(orderId)) } returns false andThen false andThen true

            val code = service.generateCode(orderId)
            code shouldMatch "\\d{6}"
        }

        test("generateCode 최대 시도 초과 → PAYMENT_CODE_GENERATION_FAILED") {
            val orderId = UUID.randomUUID()
            every { paymentCodeRepository.saveIfAbsent(any(), eq(orderId)) } returns false

            val exception = shouldThrow<CoreException> { service.generateCode(orderId) }
            exception.type shouldBe ErrorType.PAYMENT_CODE_GENERATION_FAILED
        }

        test("resolveOrderId 정상") {
            val orderId = UUID.randomUUID()
            every { paymentCodeRepository.findOrderIdByCode("123456") } returns orderId

            service.resolveOrderId("123456") shouldBe orderId
        }

        test("resolveOrderId 없는 코드 → null") {
            every { paymentCodeRepository.findOrderIdByCode("000000") } returns null

            service.resolveOrderId("000000") shouldBe null
        }

        test("invalidateCode 호출") {
            every { paymentCodeRepository.delete("123456") } returns true

            service.invalidateCode("123456")
            verify { paymentCodeRepository.delete("123456") }
        }
    })
