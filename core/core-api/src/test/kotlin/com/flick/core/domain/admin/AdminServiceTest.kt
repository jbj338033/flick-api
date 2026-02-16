package com.flick.core.domain.admin

import com.flick.core.enums.TransactionType
import com.flick.core.enums.UserRole
import com.flick.storage.db.core.entity.Booth
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.BoothRepository
import com.flick.storage.db.core.repository.OrderRepository
import com.flick.storage.db.core.repository.TransactionRepository
import com.flick.storage.db.core.repository.UserRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class AdminServiceTest :
    FunSpec({
        val userRepository = mockk<UserRepository>()
        val boothRepository = mockk<BoothRepository>()
        val orderRepository = mockk<OrderRepository>()
        val transactionRepository = mockk<TransactionRepository>()
        val service = AdminService(userRepository, boothRepository, orderRepository, transactionRepository)

        val owner = User(dauthId = "o", name = "오너", email = "o@dsm.hs.kr", role = UserRole.TEACHER)
        val booth = Booth(name = "타코야끼", user = owner)

        test("getDashboardStats 정상") {
            every { transactionRepository.sumAmountByType(TransactionType.CHARGE) } returns 50000L
            every { transactionRepository.sumAmountByType(TransactionType.GRANT) } returns 10000L
            every { transactionRepository.sumAmountByType(TransactionType.PAYMENT) } returns 30000L
            every { transactionRepository.sumAmountByType(TransactionType.REFUND) } returns 5000L
            every { boothRepository.findAll() } returns listOf(booth)
            every { orderRepository.findConfirmedOrderStatsByBooth() } returns
                listOf(
                    arrayOf(booth.id, 25000L, 5L),
                )
            every { userRepository.count() } returns 100L
            every { orderRepository.count() } returns 50L

            val stats = service.getDashboardStats()
            stats.totalUsers shouldBe 100
            stats.totalBooths shouldBe 1
            stats.totalOrders shouldBe 50
            stats.totalBalance shouldBe (50000 + 10000 - 30000 - 5000)
            stats.totalCharged shouldBe 50000
            stats.totalSpent shouldBe 30000
            stats.boothStats.size shouldBe 1
            stats.boothStats[0].totalSales shouldBe 25000
            stats.boothStats[0].orderCount shouldBe 5
        }

        test("getDashboardStats 부스 없는 경우") {
            every { transactionRepository.sumAmountByType(any()) } returns 0L
            every { boothRepository.findAll() } returns emptyList()
            every { orderRepository.findConfirmedOrderStatsByBooth() } returns emptyList()
            every { userRepository.count() } returns 0L
            every { orderRepository.count() } returns 0L

            val stats = service.getDashboardStats()
            stats.totalBooths shouldBe 0
            stats.boothStats shouldBe emptyList()
            stats.totalBalance shouldBe 0
        }
    })
