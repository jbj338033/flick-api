package com.flick.core.domain.payment

import com.flick.core.domain.order.OrderItemInput
import com.flick.core.domain.order.OrderService
import com.flick.core.domain.order.SelectedOptionSnapshot
import com.flick.core.domain.product.ProductService
import com.flick.core.enums.TransactionType
import com.flick.core.event.PaymentCompletedEvent
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.PaymentRequest
import com.flick.storage.db.core.entity.Transaction
import com.flick.storage.db.core.repository.BoothRepository
import com.flick.storage.db.core.repository.OrderItemRepository
import com.flick.storage.db.core.repository.PaymentRequestRepository
import com.flick.storage.db.core.repository.ProductOptionGroupRepository
import com.flick.storage.db.core.repository.ProductOptionRepository
import com.flick.storage.db.core.repository.ProductRepository
import com.flick.storage.db.core.repository.TransactionRepository
import com.flick.storage.db.core.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDateTime
import java.util.UUID

@Service
class PaymentService(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val paymentCodeService: PaymentCodeService,
    private val userRepository: UserRepository,
    private val boothRepository: BoothRepository,
    private val productRepository: ProductRepository,
    private val paymentRequestRepository: PaymentRequestRepository,
    private val orderItemRepository: OrderItemRepository,
    private val transactionRepository: TransactionRepository,
    private val optionGroupRepository: ProductOptionGroupRepository,
    private val optionRepository: ProductOptionRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun createPaymentRequest(createPaymentRequest: CreatePaymentRequest): PaymentRequestResult {
        val booth =
            boothRepository.findByIdOrNull(createPaymentRequest.boothId)
                ?: throw CoreException(ErrorType.BOOTH_NOT_FOUND)

        val orderItems =
            createPaymentRequest.items.map { item ->
                val product = productService.getProduct(item.productId)

                if (product.booth.id != createPaymentRequest.boothId) throw CoreException(ErrorType.PRODUCT_NOT_IN_BOOTH)
                if (product.isSoldOut) throw CoreException(ErrorType.PRODUCT_SOLD_OUT)
                product.stock?.let { stock ->
                    if (stock < item.quantity) throw CoreException(ErrorType.PRODUCT_STOCK_INSUFFICIENT)
                }

                val optionSnapshots = validateAndResolveOptions(product.id, item.options)
                val optionTotal = optionSnapshots.sumOf { it.price * it.quantity }
                val itemPrice = product.price + optionTotal

                OrderItemInput(
                    productId = product.id,
                    quantity = item.quantity,
                    price = itemPrice,
                    selectedOptions = optionSnapshots,
                )
            }
        val totalAmount = orderItems.sumOf { it.price * it.quantity }

        val order =
            orderService.createOrder(
                booth = booth,
                totalAmount = totalAmount,
                items = orderItems,
            )

        val code = paymentCodeService.generateCode(order.id)

        paymentRequestRepository.save(
            PaymentRequest(
                code = code,
                order = order,
                expiresAt = LocalDateTime.now().plusMinutes(3),
            ),
        )

        return PaymentRequestResult(
            orderId = order.id,
            code = code,
            orderNumber = order.orderNumber,
            totalAmount = totalAmount,
        )
    }

    @Transactional(readOnly = true)
    fun getPaymentRequestByCode(code: String): PaymentRequestDetailResult {
        val paymentRequest =
            paymentRequestRepository.findByCode(code)
                ?: throw CoreException(ErrorType.PAYMENT_REQUEST_NOT_FOUND)

        val order = paymentRequest.order
        val booth = order.booth
        val items = orderItemRepository.findAllByOrderId(order.id)

        return PaymentRequestDetailResult(
            orderId = order.id,
            code = paymentRequest.code,
            orderNumber = order.orderNumber,
            totalAmount = order.totalAmount,
            boothId = booth.id,
            boothName = booth.name,
            confirmed = paymentRequest.isConfirmed,
            expired = paymentRequest.expiresAt.isBefore(LocalDateTime.now()),
            expiresAt = paymentRequest.expiresAt,
            items =
                items.map { item ->
                    PaymentRequestItemResult(
                        productId = item.product.id,
                        productName = item.product.name,
                        quantity = item.quantity,
                        price = item.price,
                    )
                },
        )
    }

    @Transactional
    fun confirmPayment(
        userId: UUID,
        code: String,
    ): PaymentConfirmResult {
        val orderId =
            paymentCodeService.resolveOrderId(code)
                ?: throw CoreException(ErrorType.PAYMENT_CODE_INVALID)

        val paymentRequest =
            paymentRequestRepository.findByOrderIdForUpdate(orderId)
                ?: throw CoreException(ErrorType.PAYMENT_REQUEST_NOT_FOUND)

        if (paymentRequest.isConfirmed) throw CoreException(ErrorType.ORDER_ALREADY_CONFIRMED)
        if (paymentRequest.expiresAt.isBefore(LocalDateTime.now())) throw CoreException(ErrorType.PAYMENT_CODE_EXPIRED)

        val order = orderService.getOrder(orderId)
        val user =
            userRepository.findByIdForUpdate(userId)
                ?: throw CoreException(ErrorType.USER_NOT_FOUND)

        order.user = user

        val orderItems = orderService.getOrderItems(orderId)

        val limitedItems = orderItems.filter { it.product.purchaseLimit != null }
        if (limitedItems.isNotEmpty()) {
            val purchasedMap =
                orderItemRepository
                    .sumQuantitiesByUserIdAndProductIds(userId, limitedItems.map { it.product.id })
                    .associate { it[0] as UUID to (it[1] as Long).toInt() }

            limitedItems.forEach { item ->
                val alreadyPurchased = purchasedMap[item.product.id] ?: 0
                if (alreadyPurchased + item.quantity > item.product.purchaseLimit!!) {
                    throw CoreException(ErrorType.PURCHASE_LIMIT_EXCEEDED)
                }
            }
        }

        val products = productRepository.findByIdInForUpdate(orderItems.map { it.product.id }).associateBy { it.id }
        orderItems.forEach { item ->
            val product = products[item.product.id] ?: throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
            if (product.isSoldOut) throw CoreException(ErrorType.PRODUCT_SOLD_OUT)
            product.stock?.let { stock ->
                if (stock < item.quantity) throw CoreException(ErrorType.PRODUCT_STOCK_INSUFFICIENT)
                product.stock = stock - item.quantity
            }
        }

        if (user.balance < order.totalAmount) throw CoreException(ErrorType.INSUFFICIENT_BALANCE)

        val balanceBefore = user.balance
        user.pay(order.totalAmount)

        orderService.confirmOrder(orderId)
        paymentRequest.isConfirmed = true

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    paymentCodeService.invalidateCode(code)
                }
            },
        )

        transactionRepository.save(
            Transaction(
                type = TransactionType.PAYMENT,
                amount = order.totalAmount,
                balanceBefore = balanceBefore,
                balanceAfter = user.balance,
                user = user,
                order = order,
            ),
        )

        eventPublisher.publishEvent(
            PaymentCompletedEvent(
                orderId = orderId,
                userId = user.id,
                boothId = order.booth.id,
                totalAmount = order.totalAmount,
                orderNumber = order.orderNumber,
            ),
        )

        return PaymentConfirmResult(
            orderId = orderId,
            orderNumber = order.orderNumber,
            totalAmount = order.totalAmount,
            balanceAfter = user.balance,
        )
    }

    private fun validateAndResolveOptions(
        productId: UUID,
        selectedOptions: List<SelectedOptionInput>,
    ): List<SelectedOptionSnapshot> {
        if (selectedOptions.isEmpty()) {
            val requiredGroups = optionGroupRepository.findAllByProductIdOrderBySortOrder(productId).filter { it.isRequired }
            if (requiredGroups.isNotEmpty()) throw CoreException(ErrorType.REQUIRED_OPTION_MISSING)
            return emptyList()
        }

        val optionIds = selectedOptions.map { it.optionId }
        val options = optionRepository.findAllById(optionIds).associateBy { it.id }
        if (options.size != optionIds.size) throw CoreException(ErrorType.PRODUCT_OPTION_NOT_FOUND)

        options.values.forEach { option ->
            if (option.optionGroup.product.id != productId) throw CoreException(ErrorType.OPTION_NOT_IN_PRODUCT)
        }

        val selectionsByGroupId =
            selectedOptions.groupBy { sel ->
                options[sel.optionId]!!.optionGroup.id
            }

        val allGroups = optionGroupRepository.findAllByProductIdOrderBySortOrder(productId).associateBy { it.id }

        allGroups.values.filter { it.isRequired }.forEach { group ->
            if (group.id !in selectionsByGroupId) throw CoreException(ErrorType.REQUIRED_OPTION_MISSING)
        }

        selectionsByGroupId.forEach { (groupId, selections) ->
            val group = allGroups[groupId] ?: throw CoreException(ErrorType.OPTION_GROUP_NOT_FOUND)
            if (selections.size > group.maxSelections) throw CoreException(ErrorType.OPTION_MAX_SELECTIONS_EXCEEDED)
        }

        selectedOptions.forEach { sel ->
            val option = options[sel.optionId]!!
            if (!option.isQuantitySelectable && sel.quantity > 1) throw CoreException(ErrorType.OPTION_QUANTITY_NOT_ALLOWED)
        }

        return selectedOptions.map { sel ->
            val option = options[sel.optionId]!!
            SelectedOptionSnapshot(
                groupName = option.optionGroup.name,
                name = option.name,
                price = option.price,
                quantity = sel.quantity,
            )
        }
    }
}
