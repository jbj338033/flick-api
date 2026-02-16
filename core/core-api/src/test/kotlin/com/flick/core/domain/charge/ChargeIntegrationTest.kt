package com.flick.core.domain.charge

import com.flick.core.enums.TransactionType
import com.flick.core.enums.UserRole
import com.flick.core.support.IntegrationTest
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.TransactionRepository
import com.flick.storage.db.core.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class ChargeIntegrationTest : FunSpec() {
    @Autowired
    lateinit var chargeService: ChargeService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var transactionRepository: TransactionRepository

    init {
        test("충전 → 잔액 증가 + Transaction 기록") {
            userRepository.save(
                User(
                    dauthId = "charge-test-1",
                    name = "홍길동",
                    email = "hong@dsm.hs.kr",
                    role = UserRole.STUDENT,
                    grade = 1,
                    room = 1,
                    number = 1,
                ),
            )

            chargeService.charge(grade = 1, room = 1, number = 1, amount = 5000)

            val user = userRepository.findByGradeAndRoomAndNumber(1, 1, 1)!!
            user.balance shouldBe 5000

            val transactions = transactionRepository.findAllByUserIdOrderByCreatedAtDesc(user.id)
            transactions.size shouldBe 1
            transactions[0].type shouldBe TransactionType.CHARGE
            transactions[0].amount shouldBe 5000
            transactions[0].balanceBefore shouldBe 0
            transactions[0].balanceAfter shouldBe 5000
        }

        test("존재하지 않는 학번 → USER_NOT_FOUND") {
            val exception =
                shouldThrow<CoreException> {
                    chargeService.charge(grade = 9, room = 9, number = 99, amount = 5000)
                }
            exception.type shouldBe ErrorType.USER_NOT_FOUND
        }
    }
}
