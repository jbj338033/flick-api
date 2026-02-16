package com.flick.core.domain.admin

import java.util.UUID

data class DashboardStats(
    val totalUsers: Int,
    val totalBooths: Int,
    val totalOrders: Int,
    val totalBalance: Int,
    val totalCharged: Int,
    val totalSpent: Int,
    val boothStats: List<BoothStat>,
)

data class BoothStat(
    val boothId: UUID,
    val name: String,
    val totalSales: Int,
    val orderCount: Int,
)
