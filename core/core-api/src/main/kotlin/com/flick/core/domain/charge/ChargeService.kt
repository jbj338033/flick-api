package com.flick.core.domain.charge

import com.flick.core.enums.TransactionType
import com.flick.core.event.PointChargedEvent
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Transaction
import com.flick.storage.db.core.repository.TransactionRepository
import com.flick.storage.db.core.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChargeService(
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun charge(
        grade: Int,
        room: Int,
        number: Int,
        amount: Int,
    ) {
        val user =
            userRepository
                .findByGradeAndRoomAndNumber(grade, room, number)
                ?.let { userRepository.findByIdForUpdate(it.id) }
                ?: throw CoreException(ErrorType.USER_NOT_FOUND)

        val balanceBefore = user.balance
        user.charge(amount)
        val balanceAfter = user.balance

        transactionRepository.save(
            Transaction(
                type = TransactionType.CHARGE,
                amount = amount,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                user = user,
            ),
        )

        eventPublisher.publishEvent(
            PointChargedEvent(
                userId = user.id,
                amount = amount,
                balanceAfter = balanceAfter,
            ),
        )
    }
}
