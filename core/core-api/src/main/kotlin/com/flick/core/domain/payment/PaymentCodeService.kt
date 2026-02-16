package com.flick.core.domain.payment

import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.redis.PaymentCodeRepository
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.random.Random

private const val MAX_ATTEMPTS = 10

@Service
class PaymentCodeService(
    private val paymentCodeRepository: PaymentCodeRepository,
) {
    fun generateCode(orderId: UUID): String {
        repeat(MAX_ATTEMPTS) {
            val code = Random.nextInt(100000, 999999).toString()
            if (paymentCodeRepository.saveIfAbsent(code, orderId)) {
                return code
            }
        }
        throw CoreException(ErrorType.PAYMENT_CODE_GENERATION_FAILED)
    }

    fun resolveOrderId(code: String): UUID? = paymentCodeRepository.findOrderIdByCode(code)

    fun invalidateCode(code: String) = paymentCodeRepository.delete(code)
}
