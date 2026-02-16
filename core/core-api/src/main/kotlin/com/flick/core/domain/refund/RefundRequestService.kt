package com.flick.core.domain.refund

import com.flick.core.enums.Bank
import com.flick.core.enums.TransactionType
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.RefundRequest
import com.flick.storage.db.core.entity.Transaction
import com.flick.storage.db.core.repository.RefundRequestRepository
import com.flick.storage.db.core.repository.TransactionRepository
import com.flick.storage.db.core.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class RefundRequestService(
    private val refundRequestRepository: RefundRequestRepository,
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
) {
    @Transactional
    fun createRefundRequest(
        userId: UUID,
        bank: Bank,
        accountNumber: String,
    ): RefundRequest {
        val user =
            userRepository.findByIdForUpdate(userId)
                ?: throw CoreException(ErrorType.USER_NOT_FOUND)

        if (refundRequestRepository.existsByUserId(userId)) {
            throw CoreException(ErrorType.REFUND_ALREADY_REQUESTED)
        }

        val totalCharged = transactionRepository.sumAmountByUserIdAndType(user.id, TransactionType.CHARGE)
        val totalRefunded = transactionRepository.sumAmountByUserIdAndType(user.id, TransactionType.REFUND)
        val refundableAmount = minOf(user.balance, (totalCharged - totalRefunded).toInt())
        if (refundableAmount <= 0) throw CoreException(ErrorType.REFUND_NO_BALANCE)

        val balanceBefore = user.balance
        user.pay(refundableAmount)

        transactionRepository.save(
            Transaction(
                type = TransactionType.REFUND,
                amount = refundableAmount,
                balanceBefore = balanceBefore,
                balanceAfter = user.balance,
                user = user,
            ),
        )

        return refundRequestRepository.save(
            RefundRequest(
                bank = bank,
                accountNumber = accountNumber,
                amount = refundableAmount,
                user = user,
            ),
        )
    }

    fun getAllRefundRequests(): List<RefundRequest> = refundRequestRepository.findAllByOrderByCreatedAtDesc()
}
