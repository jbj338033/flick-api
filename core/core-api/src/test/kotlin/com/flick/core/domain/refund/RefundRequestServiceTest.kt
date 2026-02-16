package com.flick.core.domain.refund

import com.flick.core.enums.Bank
import com.flick.core.enums.TransactionType
import com.flick.core.enums.UserRole
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.RefundRequest
import com.flick.storage.db.core.entity.Transaction
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.RefundRequestRepository
import com.flick.storage.db.core.repository.TransactionRepository
import com.flick.storage.db.core.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.UUID

class RefundRequestServiceTest :
    FunSpec({
        val refundRequestRepository = mockk<RefundRequestRepository>()
        val userRepository = mockk<UserRepository>()
        val transactionRepository = mockk<TransactionRepository>()
        val service = RefundRequestService(refundRequestRepository, userRepository, transactionRepository)

        test("createRefundRequest 정상") {
            val user = User(dauthId = "u1", name = "홍길동", email = "h@dsm.hs.kr", role = UserRole.STUDENT, balance = 5000)
            every { userRepository.findByIdForUpdate(user.id) } returns user
            every { refundRequestRepository.existsByUserId(user.id) } returns false
            every { transactionRepository.sumAmountByUserIdAndType(user.id, TransactionType.CHARGE) } returns 10000L
            every { transactionRepository.sumAmountByUserIdAndType(user.id, TransactionType.REFUND) } returns 0L
            every { transactionRepository.save(any<Transaction>()) } answers { firstArg() }
            every { refundRequestRepository.save(any<RefundRequest>()) } answers { firstArg() }

            val result = service.createRefundRequest(user.id, Bank.KAKAO, "123-456")
            result.amount shouldBe 5000
            result.bank shouldBe Bank.KAKAO
            user.balance shouldBe 0
        }

        test("createRefundRequest 환불 가능 금액 = min(잔액, 충전-환불 차이)") {
            val user = User(dauthId = "u2", name = "김철수", email = "k@dsm.hs.kr", role = UserRole.STUDENT, balance = 8000)
            every { userRepository.findByIdForUpdate(user.id) } returns user
            every { refundRequestRepository.existsByUserId(user.id) } returns false
            every { transactionRepository.sumAmountByUserIdAndType(user.id, TransactionType.CHARGE) } returns 6000L
            every { transactionRepository.sumAmountByUserIdAndType(user.id, TransactionType.REFUND) } returns 1000L
            every { transactionRepository.save(any<Transaction>()) } answers { firstArg() }
            every { refundRequestRepository.save(any<RefundRequest>()) } answers { firstArg() }

            val result = service.createRefundRequest(user.id, Bank.KAKAO, "789-012")
            result.amount shouldBe 5000
        }

        test("createRefundRequest USER_NOT_FOUND") {
            val id = UUID.randomUUID()
            every { userRepository.findByIdForUpdate(id) } returns null

            shouldThrow<CoreException> {
                service.createRefundRequest(id, Bank.KAKAO, "000")
            }.type shouldBe ErrorType.USER_NOT_FOUND
        }

        test("createRefundRequest REFUND_ALREADY_REQUESTED") {
            val user = User(dauthId = "u3", name = "이미요청", email = "r@dsm.hs.kr", role = UserRole.STUDENT, balance = 5000)
            every { userRepository.findByIdForUpdate(user.id) } returns user
            every { refundRequestRepository.existsByUserId(user.id) } returns true

            shouldThrow<CoreException> {
                service.createRefundRequest(user.id, Bank.KAKAO, "000")
            }.type shouldBe ErrorType.REFUND_ALREADY_REQUESTED
        }

        test("createRefundRequest REFUND_NO_BALANCE") {
            val user = User(dauthId = "u4", name = "잔액없음", email = "n@dsm.hs.kr", role = UserRole.STUDENT, balance = 0)
            every { userRepository.findByIdForUpdate(user.id) } returns user
            every { refundRequestRepository.existsByUserId(user.id) } returns false
            every { transactionRepository.sumAmountByUserIdAndType(user.id, TransactionType.CHARGE) } returns 0L
            every { transactionRepository.sumAmountByUserIdAndType(user.id, TransactionType.REFUND) } returns 0L

            shouldThrow<CoreException> {
                service.createRefundRequest(user.id, Bank.KAKAO, "000")
            }.type shouldBe ErrorType.REFUND_NO_BALANCE
        }

        test("getAllRefundRequests 정상") {
            every { refundRequestRepository.findAllByOrderByCreatedAtDesc() } returns emptyList()

            service.getAllRefundRequests() shouldBe emptyList()
        }
    })
