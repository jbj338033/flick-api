package com.flick.core.domain.charge

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

class ChargeServiceTest :
    FunSpec({
        val userRepository = mockk<UserRepository>()
        val transactionRepository = mockk<TransactionRepository>()
        val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
        val service = ChargeService(userRepository, transactionRepository, eventPublisher)

        test("charge 정상 → 잔액 증가, 트랜잭션 저장, 이벤트 발행") {
            val user =
                User(
                    dauthId = "u1",
                    name = "홍길동",
                    email = "h@dsm.hs.kr",
                    role = UserRole.STUDENT,
                    balance = 1000,
                    grade = 1,
                    room = 1,
                    number = 1,
                )
            every { userRepository.findByGradeAndRoomAndNumber(1, 1, 1) } returns user
            every { userRepository.findByIdForUpdate(user.id) } returns user
            every { transactionRepository.save(any<Transaction>()) } answers { firstArg() }

            service.charge(grade = 1, room = 1, number = 1, amount = 5000)

            user.balance shouldBe 6000

            val txSlot = slot<Transaction>()
            verify { transactionRepository.save(capture(txSlot)) }
            txSlot.captured.balanceBefore shouldBe 1000
            txSlot.captured.balanceAfter shouldBe 6000
            txSlot.captured.amount shouldBe 5000

            val eventSlot = slot<PointChargedEvent>()
            verify { eventPublisher.publishEvent(capture(eventSlot)) }
            eventSlot.captured.amount shouldBe 5000
            eventSlot.captured.balanceAfter shouldBe 6000
        }

        test("charge 존재하지 않는 학번 → USER_NOT_FOUND") {
            every { userRepository.findByGradeAndRoomAndNumber(9, 9, 99) } returns null

            val exception = shouldThrow<CoreException> { service.charge(9, 9, 99, 5000) }
            exception.type shouldBe ErrorType.USER_NOT_FOUND
        }
    })
