package com.flick.core.domain.admin

import com.flick.core.enums.TransactionType
import com.flick.storage.db.core.repository.BoothRepository
import com.flick.storage.db.core.repository.OrderRepository
import com.flick.storage.db.core.repository.TransactionRepository
import com.flick.storage.db.core.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AdminService(
    private val userRepository: UserRepository,
    private val boothRepository: BoothRepository,
    private val orderRepository: OrderRepository,
    private val transactionRepository: TransactionRepository,
) {
    fun getDashboardStats(): DashboardStats {
        val totalCharged = transactionRepository.sumAmountByType(TransactionType.CHARGE)
        val totalGrant = transactionRepository.sumAmountByType(TransactionType.GRANT)
        val totalSpent = transactionRepository.sumAmountByType(TransactionType.PAYMENT)
        val totalRefunded = transactionRepository.sumAmountByType(TransactionType.REFUND)

        val booths = boothRepository.findAll()
        val statsMap =
            orderRepository.findConfirmedOrderStatsByBooth().associate { row ->
                val boothId = row[0] as UUID
                val totalSales = (row[1] as Long).toInt()
                val orderCount = (row[2] as Long).toInt()
                boothId to (totalSales to orderCount)
            }

        val boothStats =
            booths.map { booth ->
                val (totalSales, orderCount) = statsMap[booth.id] ?: (0 to 0)
                BoothStat(
                    boothId = booth.id,
                    name = booth.name,
                    totalSales = totalSales,
                    orderCount = orderCount,
                )
            }

        return DashboardStats(
            totalUsers = userRepository.count().toInt(),
            totalBooths = booths.size,
            totalOrders = orderRepository.count().toInt(),
            totalBalance = (totalCharged + totalGrant - totalSpent - totalRefunded).toInt(),
            totalCharged = totalCharged.toInt(),
            totalSpent = totalSpent.toInt(),
            boothStats = boothStats,
        )
    }
}
