package com.flick.core.domain.grant

import com.flick.core.enums.UserRole
import com.flick.core.event.PointChargedEvent
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Transaction
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.TransactionRepository
import com.flick.storage.db.core.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher

class GrantServiceTest :
    FunSpec({
        val userRepository = mockk<UserRepository>()
        val transactionRepository = mockk<TransactionRepository>()
        val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
        val service = GrantService(userRepository, transactionRepository, eventPublisher)

        test("claimGrant 정상 → 잔액 증가, 트랜잭션, 이벤트") {
            val user = User(dauthId = "u1", name = "테스터", email = "t@dsm.hs.kr", role = UserRole.STUDENT, balance = 500)
            every { userRepository.findByIdForUpdate(user.id) } returns user
            every { transactionRepository.save(any<Transaction>()) } answers { firstArg() }

            service.claimGrant(user.id)

            user.balance shouldBe 500 + User.INITIAL_BALANCE
            user.isGrantClaimed shouldBe true

            val eventSlot = slot<PointChargedEvent>()
            verify { eventPublisher.publishEvent(capture(eventSlot)) }
            eventSlot.captured.amount shouldBe User.INITIAL_BALANCE
        }

        test("claimGrant 존재하지 않는 사용자 → USER_NOT_FOUND") {
            val id = java.util.UUID.randomUUID()
            every { userRepository.findByIdForUpdate(id) } returns null

            val exception = shouldThrow<CoreException> { service.claimGrant(id) }
            exception.type shouldBe ErrorType.USER_NOT_FOUND
        }

        test("claimGrant 이미 수령 → GRANT_ALREADY_CLAIMED") {
            val user = User(dauthId = "u2", name = "이미수령", email = "c@dsm.hs.kr", role = UserRole.STUDENT, isGrantClaimed = true)
            every { userRepository.findByIdForUpdate(user.id) } returns user

            val exception = shouldThrow<CoreException> { service.claimGrant(user.id) }
            exception.type shouldBe ErrorType.GRANT_ALREADY_CLAIMED
        }
    })
