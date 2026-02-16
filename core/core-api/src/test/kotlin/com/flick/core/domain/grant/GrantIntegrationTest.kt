package com.flick.core.domain.grant

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
import org.springframework.data.repository.findByIdOrNull

@IntegrationTest
class GrantIntegrationTest : FunSpec() {
    @Autowired
    lateinit var grantService: GrantService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var transactionRepository: TransactionRepository

    init {
        test("초기 잔액 수령 → 잔액 증가 + Transaction 기록") {
            val user =
                userRepository.save(
                    User(dauthId = "grant-test-1", name = "수령자", email = "g1@dsm.hs.kr", role = UserRole.STUDENT),
                )

            grantService.claimGrant(user.id)

            val updated = userRepository.findByIdOrNull(user.id)!!
            updated.balance shouldBe User.INITIAL_BALANCE
            updated.isGrantClaimed shouldBe true

            val transactions = transactionRepository.findAllByUserIdOrderByCreatedAtDesc(user.id)
            transactions.size shouldBe 1
            transactions[0].type shouldBe TransactionType.GRANT
            transactions[0].amount shouldBe User.INITIAL_BALANCE
        }

        test("이미 수령 시 GRANT_ALREADY_CLAIMED") {
            val user =
                userRepository.save(
                    User(dauthId = "grant-test-2", name = "이미수령", email = "g2@dsm.hs.kr", role = UserRole.STUDENT, isGrantClaimed = true),
                )

            val exception = shouldThrow<CoreException> { grantService.claimGrant(user.id) }
            exception.type shouldBe ErrorType.GRANT_ALREADY_CLAIMED
        }
    }
}
