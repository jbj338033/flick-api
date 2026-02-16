package com.flick.storage.db.core.repository

import com.flick.storage.db.core.entity.OrderItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface OrderItemRepository : JpaRepository<OrderItem, UUID> {
    fun findAllByOrderId(orderId: UUID): List<OrderItem>

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.product WHERE oi.order.id = :orderId")
    fun findAllByOrderIdWithProduct(orderId: UUID): List<OrderItem>

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.product WHERE oi.order.id IN :orderIds")
    fun findAllByOrderIdInWithProduct(orderIds: List<UUID>): List<OrderItem>

    @Query(
        "SELECT oi.product.id, COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
            "WHERE oi.order.user.id = :userId AND oi.product.id IN :productIds AND oi.order.status = 'CONFIRMED' " +
            "GROUP BY oi.product.id",
    )
    fun sumQuantitiesByUserIdAndProductIds(
        userId: UUID,
        productIds: List<UUID>,
    ): List<Array<Any>>

    @Query(
        "SELECT b.name, p.name, MAX(oi.price), SUM(oi.quantity), SUM(oi.price * oi.quantity) " +
            "FROM OrderItem oi JOIN oi.order o JOIN o.booth b JOIN oi.product p " +
            "WHERE o.status = 'CONFIRMED' GROUP BY b.name, p.name ORDER BY b.name, p.name",
    )
    fun findBoothProductSalesAggregated(): List<Array<Any>>

    @Query(
        "SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END FROM OrderItem oi " +
            "WHERE oi.product.id = :productId AND oi.order.status IN ('PENDING', 'CONFIRMED')",
    )
    fun existsActiveOrdersByProductId(productId: UUID): Boolean
}
