package com.flick.core.domain.grant

import com.flick.core.enums.TransactionType
import com.flick.core.event.PointChargedEvent
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Transaction
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.TransactionRepository
import com.flick.storage.db.core.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GrantService(
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun claimGrant(userId: UUID) {
        val user =
            userRepository.findByIdForUpdate(userId)
                ?: throw CoreException(ErrorType.USER_NOT_FOUND)

        if (user.isGrantClaimed) throw CoreException(ErrorType.GRANT_ALREADY_CLAIMED)

        val balanceBefore = user.balance
        user.claimGrant()
        val balanceAfter = user.balance

        transactionRepository.save(
            Transaction(
                type = TransactionType.GRANT,
                amount = User.INITIAL_BALANCE,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                user = user,
            ),
        )

        eventPublisher.publishEvent(
            PointChargedEvent(
                userId = user.id,
                amount = User.INITIAL_BALANCE,
                balanceAfter = balanceAfter,
            ),
        )
    }
}
