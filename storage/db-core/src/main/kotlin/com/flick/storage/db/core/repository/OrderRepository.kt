package com.flick.storage.db.core.repository

import com.flick.storage.db.core.entity.Order
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import java.util.UUID

interface OrderRepository : JpaRepository<Order, UUID> {
    fun findAllByUserId(userId: UUID): List<Order>

    fun findAllByBoothId(boothId: UUID): List<Order>

    @Query("SELECT COALESCE(MAX(o.orderNumber), 0) FROM Order o WHERE o.booth.id = :boothId")
    fun findMaxOrderNumberByBoothId(boothId: UUID): Int

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :orderId")
    fun findByIdForUpdate(orderId: UUID): Order?

    fun findAllByOrderByCreatedAtDesc(): List<Order>

    @Query(
        "SELECT o.booth.id, COALESCE(SUM(o.totalAmount), 0), COUNT(o) " +
            "FROM Order o WHERE o.status = 'CONFIRMED' GROUP BY o.booth.id",
    )
    fun findConfirmedOrderStatsByBooth(): List<Array<Any>>

    @Modifying
    @Query(
        "UPDATE Order o SET o.status = 'CANCELLED' " +
            "WHERE o.status = 'PENDING' AND o.id IN (SELECT pr.order.id FROM PaymentRequest pr WHERE pr.isConfirmed = false AND pr.expiresAt < :now)",
    )
    fun cancelExpiredPaymentOrders(now: LocalDateTime): Int
}
