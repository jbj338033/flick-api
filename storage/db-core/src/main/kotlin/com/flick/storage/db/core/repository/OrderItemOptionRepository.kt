package com.flick.storage.db.core.repository

import com.flick.storage.db.core.entity.OrderItemOption
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OrderItemOptionRepository : JpaRepository<OrderItemOption, UUID> {
    fun findAllByOrderItemIdIn(orderItemIds: List<UUID>): List<OrderItemOption>
}
