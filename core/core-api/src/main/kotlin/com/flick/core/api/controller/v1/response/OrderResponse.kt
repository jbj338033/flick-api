package com.flick.core.api.controller.v1.response

import com.flick.core.enums.OrderStatus
import com.flick.storage.db.core.entity.Order
import com.flick.storage.db.core.entity.OrderItem
import com.flick.storage.db.core.entity.OrderItemOption
import java.time.LocalDateTime
import java.util.UUID

data class OrderResponse(
    val id: UUID,
    val orderNumber: Int,
    val totalAmount: Int,
    val status: OrderStatus,
    val booth: BoothRef,
    val createdAt: LocalDateTime,
    val items: List<OrderItemResponse>,
)

data class BoothRef(
    val id: UUID,
    val name: String,
)

fun Order.toResponse(
    items: List<OrderItem>,
    optionsByOrderItemId: Map<UUID, List<OrderItemOption>> = emptyMap(),
) = OrderResponse(
    id = id,
    orderNumber = orderNumber,
    totalAmount = totalAmount,
    status = status,
    booth = BoothRef(id = booth.id, name = booth.name),
    createdAt = createdAt,
    items =
        items.map { item ->
            val itemOptions = optionsByOrderItemId[item.id] ?: emptyList()
            OrderItemResponse(
                product = ProductRef(id = item.product.id, name = item.product.name),
                quantity = item.quantity,
                price = item.price,
                options =
                    itemOptions
                        .groupBy { it.groupName }
                        .map { (groupName, opts) ->
                            OrderItemOptionGroupResponse(
                                groupName = groupName,
                                selections =
                                    opts.map { opt ->
                                        OrderItemOptionResponse(
                                            name = opt.name,
                                            price = opt.price,
                                            quantity = opt.quantity,
                                        )
                                    },
                            )
                        },
            )
        },
)
