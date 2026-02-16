package com.flick.core.domain.order

import com.flick.core.api.controller.v1.response.OrderResponse
import com.flick.core.api.controller.v1.response.toResponse
import com.flick.core.enums.OrderStatus
import com.flick.core.enums.TransactionType
import com.flick.core.enums.UserRole
import com.flick.core.event.OrderCancelledEvent
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Booth
import com.flick.storage.db.core.entity.Order
import com.flick.storage.db.core.entity.OrderItem
import com.flick.storage.db.core.entity.OrderItemOption
import com.flick.storage.db.core.entity.Transaction
import com.flick.storage.db.core.repository.OrderItemOptionRepository
import com.flick.storage.db.core.repository.OrderItemRepository
import com.flick.storage.db.core.repository.OrderRepository
import com.flick.storage.db.core.repository.ProductRepository
import com.flick.storage.db.core.repository.TransactionRepository
import com.flick.storage.db.core.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val orderItemOptionRepository: OrderItemOptionRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    fun getOrder(orderId: UUID): Order =
        orderRepository.findByIdOrNull(orderId)
            ?: throw CoreException(ErrorType.ORDER_NOT_FOUND)

    fun getOrdersByUser(userId: UUID): List<Order> = orderRepository.findAllByUserId(userId)

    fun getOrdersByBooth(boothId: UUID): List<Order> = orderRepository.findAllByBoothId(boothId)

    fun getAllOrders(): List<Order> = orderRepository.findAllByOrderByCreatedAtDesc()

    fun getOrderItems(orderId: UUID): List<OrderItem> = orderItemRepository.findAllByOrderId(orderId)

    fun getOrderItemsWithProduct(orderId: UUID): List<OrderItem> = orderItemRepository.findAllByOrderIdWithProduct(orderId)

    fun getOrdersWithItemsByBooth(boothId: UUID): List<OrderResponse> {
        val orders = orderRepository.findAllByBoothId(boothId)
        return toOrderResponses(orders)
    }

    fun getOrdersWithItemsByUser(userId: UUID): List<OrderResponse> {
        val orders = orderRepository.findAllByUserId(userId)
        return toOrderResponses(orders)
    }

    fun getAllOrdersWithItems(): List<OrderResponse> {
        val orders = orderRepository.findAllByOrderByCreatedAtDesc()
        return toOrderResponses(orders)
    }

    private fun toOrderResponses(orders: List<Order>): List<OrderResponse> {
        if (orders.isEmpty()) return emptyList()
        val allItems = orderItemRepository.findAllByOrderIdInWithProduct(orders.map { it.id })
        val itemsByOrderId = allItems.groupBy { it.order.id }
        val allItemIds = allItems.map { it.id }
        val optionsByItemId =
            if (allItemIds.isNotEmpty()) {
                orderItemOptionRepository.findAllByOrderItemIdIn(allItemIds).groupBy { it.orderItem.id }
            } else {
                emptyMap()
            }
        return orders.map { order -> order.toResponse(itemsByOrderId[order.id] ?: emptyList(), optionsByItemId) }
    }

    @Transactional
    fun createOrder(
        booth: Booth,
        totalAmount: Int,
        items: List<OrderItemInput>,
    ): Order {
        val orderNumber = orderRepository.findMaxOrderNumberByBoothId(booth.id) + 1

        val order =
            orderRepository.save(
                Order(
                    orderNumber = orderNumber,
                    totalAmount = totalAmount,
                    booth = booth,
                ),
            )

        val orderItems =
            items.map { item ->
                OrderItem(
                    quantity = item.quantity,
                    price = item.price,
                    order = order,
                    product = productRepository.getReferenceById(item.productId),
                )
            }
        orderItemRepository.saveAll(orderItems)

        items.zip(orderItems).forEach { (input, orderItem) ->
            if (input.selectedOptions.isNotEmpty()) {
                val optionEntities =
                    input.selectedOptions.map { opt ->
                        OrderItemOption(
                            groupName = opt.groupName,
                            name = opt.name,
                            price = opt.price,
                            quantity = opt.quantity,
                            orderItem = orderItem,
                        )
                    }
                orderItemOptionRepository.saveAll(optionEntities)
            }
        }

        return order
    }

    @Transactional
    fun confirmOrder(orderId: UUID): Order {
        val order = getOrder(orderId)
        if (order.status == OrderStatus.CONFIRMED) throw CoreException(ErrorType.ORDER_ALREADY_CONFIRMED)
        order.status = OrderStatus.CONFIRMED
        return order
    }

    @Transactional
    fun cancelOrder(
        userId: UUID,
        orderId: UUID,
    ) {
        val order =
            orderRepository.findByIdForUpdate(orderId)
                ?: throw CoreException(ErrorType.ORDER_NOT_FOUND)
        if (order.status != OrderStatus.CONFIRMED) throw CoreException(ErrorType.ORDER_NOT_CANCELLABLE)

        val requestUser =
            userRepository.findByIdOrNull(userId)
                ?: throw CoreException(ErrorType.USER_NOT_FOUND)

        if (requestUser.role != UserRole.ADMIN && order.booth.user.id != userId) {
            throw CoreException(ErrorType.ORDER_CANCEL_FORBIDDEN)
        }

        order.status = OrderStatus.CANCELLED

        val buyer = order.user ?: throw CoreException(ErrorType.USER_NOT_FOUND)
        val user =
            userRepository.findByIdForUpdate(buyer.id)
                ?: throw CoreException(ErrorType.USER_NOT_FOUND)
        val balanceBefore = user.balance
        user.charge(order.totalAmount)

        val orderItems = orderItemRepository.findAllByOrderId(orderId)
        val products = productRepository.findByIdInForUpdate(orderItems.map { it.product.id }).associateBy { it.id }
        orderItems.forEach { item ->
            val product = products[item.product.id] ?: throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
            product.stock?.let { stock -> product.stock = stock + item.quantity }
        }

        transactionRepository.save(
            Transaction(
                type = TransactionType.REFUND,
                amount = order.totalAmount,
                balanceBefore = balanceBefore,
                balanceAfter = user.balance,
                user = user,
                order = order,
            ),
        )

        eventPublisher.publishEvent(
            OrderCancelledEvent(
                orderId = orderId,
                userId = buyer.id,
                boothId = order.booth.id,
                totalAmount = order.totalAmount,
                orderNumber = order.orderNumber,
            ),
        )
    }
}
