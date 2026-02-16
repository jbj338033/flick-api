package com.flick.core.domain.refund

import com.flick.core.enums.Bank
import com.flick.core.enums.TransactionType
import com.flick.core.enums.UserRole
import com.flick.core.support.IntegrationTest
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Transaction
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.RefundRequestRepository
import com.flick.storage.db.core.repository.TransactionRepository
import com.flick.storage.db.core.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

@IntegrationTest
class RefundRequestIntegrationTest : FunSpec() {
    @Autowired
    lateinit var refundRequestService: RefundRequestService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var transactionRepository: TransactionRepository

    @Autowired
    lateinit var refundRequestRepository: RefundRequestRepository

    init {
        test("환불 요청 → 잔액 차감 + Transaction + RefundRequest 저장") {
            val user =
                userRepository.save(
                    User(dauthId = "refund-test-1", name = "환불자", email = "r1@dsm.hs.kr", role = UserRole.STUDENT, balance = 5000),
                )
            transactionRepository.save(
                Transaction(type = TransactionType.CHARGE, amount = 5000, balanceBefore = 0, balanceAfter = 5000, user = user),
            )

            val result = refundRequestService.createRefundRequest(user.id, Bank.KAKAO, "1234567890")
            result.amount shouldBe 5000
            result.bank shouldBe Bank.KAKAO

            val updated = userRepository.findByIdOrNull(user.id)!!
            updated.balance shouldBe 0

            val transactions = transactionRepository.findAllByUserIdOrderByCreatedAtDesc(user.id)
            transactions.any { it.type == TransactionType.REFUND && it.amount == 5000 } shouldBe true
        }

        test("중복 요청 시 REFUND_ALREADY_REQUESTED") {
            val user =
                userRepository.save(
                    User(dauthId = "refund-test-2", name = "중복환불", email = "r2@dsm.hs.kr", role = UserRole.STUDENT, balance = 3000),
                )
            transactionRepository.save(
                Transaction(type = TransactionType.CHARGE, amount = 3000, balanceBefore = 0, balanceAfter = 3000, user = user),
            )

            refundRequestService.createRefundRequest(user.id, Bank.KB, "9876543210")

            val exception =
                shouldThrow<CoreException> {
                    refundRequestService.createRefundRequest(user.id, Bank.KB, "9876543210")
                }
            exception.type shouldBe ErrorType.REFUND_ALREADY_REQUESTED
        }

        test("잔액 없는 사용자 시 REFUND_NO_BALANCE") {
            val user =
                userRepository.save(
                    User(dauthId = "refund-test-3", name = "빈잔액", email = "r3@dsm.hs.kr", role = UserRole.STUDENT, balance = 0),
                )

            val exception =
                shouldThrow<CoreException> {
                    refundRequestService.createRefundRequest(user.id, Bank.KAKAO, "0000000000")
                }
            exception.type shouldBe ErrorType.REFUND_NO_BALANCE
        }
    }
}
