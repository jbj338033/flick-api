package com.flick.core.domain.entity

import com.flick.core.enums.UserRole
import com.flick.storage.db.core.entity.User
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UserEntityTest :
    FunSpec({
        fun createUser(
            balance: Int = 0,
            isGrantClaimed: Boolean = false,
        ) = User(
            dauthId = "test",
            name = "테스터",
            email = "test@dsm.hs.kr",
            role = UserRole.STUDENT,
            balance = balance,
            isGrantClaimed = isGrantClaimed,
        )

        context("charge") {
            test("정상 충전") {
                val user = createUser(balance = 1000)
                user.charge(500)
                user.balance shouldBe 1500
            }

            test("0 이하 금액 → IllegalArgumentException") {
                val user = createUser()
                shouldThrow<IllegalArgumentException> { user.charge(0) }
                shouldThrow<IllegalArgumentException> { user.charge(-1) }
            }
        }

        context("pay") {
            test("정상 결제") {
                val user = createUser(balance = 5000)
                user.pay(3000)
                user.balance shouldBe 2000
            }

            test("잔액 부족 → IllegalStateException") {
                val user = createUser(balance = 100)
                shouldThrow<IllegalStateException> { user.pay(200) }
            }

            test("0 이하 금액 → IllegalArgumentException") {
                val user = createUser(balance = 1000)
                shouldThrow<IllegalArgumentException> { user.pay(0) }
                shouldThrow<IllegalArgumentException> { user.pay(-1) }
            }
        }

        context("claimGrant") {
            test("정상 수령") {
                val user = createUser(balance = 0)
                user.claimGrant()
                user.balance shouldBe User.INITIAL_BALANCE
                user.isGrantClaimed shouldBe true
            }

            test("이미 수령 → IllegalStateException") {
                val user = createUser(isGrantClaimed = true)
                shouldThrow<IllegalStateException> { user.claimGrant() }
            }

            test("기존 잔액에 추가") {
                val user = createUser(balance = 500)
                user.claimGrant()
                user.balance shouldBe 500 + User.INITIAL_BALANCE
            }
        }
    })
