package com.flick.core.domain.order

import com.flick.core.enums.OrderStatus
import com.flick.core.enums.UserRole
import com.flick.core.event.OrderCancelledEvent
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Booth
import com.flick.storage.db.core.entity.Order
import com.flick.storage.db.core.entity.OrderItem
import com.flick.storage.db.core.entity.OrderItemOption
import com.flick.storage.db.core.entity.Product
import com.flick.storage.db.core.entity.Transaction
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.OrderItemOptionRepository
import com.flick.storage.db.core.repository.OrderItemRepository
import com.flick.storage.db.core.repository.OrderRepository
import com.flick.storage.db.core.repository.ProductRepository
import com.flick.storage.db.core.repository.TransactionRepository
import com.flick.storage.db.core.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID

class OrderServiceTest :
    FunSpec({
        val orderRepository = mockk<OrderRepository>()
        val orderItemRepository = mockk<OrderItemRepository>()
        val orderItemOptionRepository = mockk<OrderItemOptionRepository>()
        val productRepository = mockk<ProductRepository>()
        val userRepository = mockk<UserRepository>()
        val transactionRepository = mockk<TransactionRepository>()
        val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
        val service =
            OrderService(
                orderRepository,
                orderItemRepository,
                orderItemOptionRepository,
                productRepository,
                userRepository,
                transactionRepository,
                eventPublisher,
            )

        val owner = User(dauthId = "o1", name = "오너", email = "o@dsm.hs.kr", role = UserRole.TEACHER)
        val buyer = User(dauthId = "b1", name = "구매자", email = "b@dsm.hs.kr", role = UserRole.STUDENT, balance = 10000)
        val admin = User(dauthId = "a1", name = "관리자", email = "a@dsm.hs.kr", role = UserRole.ADMIN)
        val booth = Booth(name = "부스", user = owner)
        val product = Product(name = "타코야끼", price = 3000, stock = 10, booth = booth)

        test("getOrder 정상") {
            val order = Order(orderNumber = 1, totalAmount = 3000, booth = booth)
            every { orderRepository.findByIdOrNull(order.id) } returns order

            service.getOrder(order.id).orderNumber shouldBe 1
        }

        test("getOrder ORDER_NOT_FOUND") {
            val id = UUID.randomUUID()
            every { orderRepository.findByIdOrNull(id) } returns null

            shouldThrow<CoreException> { service.getOrder(id) }.type shouldBe ErrorType.ORDER_NOT_FOUND
        }

        test("getOrdersByUser") {
            every { orderRepository.findAllByUserId(buyer.id) } returns emptyList()
            service.getOrdersByUser(buyer.id) shouldBe emptyList()
        }

        test("getOrdersByBooth") {
            every { orderRepository.findAllByBoothId(booth.id) } returns emptyList()
            service.getOrdersByBooth(booth.id) shouldBe emptyList()
        }

        test("getAllOrders") {
            every { orderRepository.findAllByOrderByCreatedAtDesc() } returns emptyList()
            service.getAllOrders() shouldBe emptyList()
        }

        test("createOrder 정상 → 주문번호 자동 증가") {
            every { orderRepository.findMaxOrderNumberByBoothId(booth.id) } returns 5
            every { orderRepository.save(any<Order>()) } answers { firstArg() }
            every { productRepository.getReferenceById(product.id) } returns product
            every { orderItemRepository.saveAll(any<List<OrderItem>>()) } answers { firstArg() }

            val items = listOf(OrderItemInput(productId = product.id, quantity = 2, price = 3000))
            val order = service.createOrder(booth, 6000, items)

            order.orderNumber shouldBe 6
            order.totalAmount shouldBe 6000
        }

        test("createOrder 옵션 포함") {
            every { orderRepository.findMaxOrderNumberByBoothId(booth.id) } returns 0
            every { orderRepository.save(any<Order>()) } answers { firstArg() }
            every { productRepository.getReferenceById(product.id) } returns product
            every { orderItemRepository.saveAll(any<List<OrderItem>>()) } answers { firstArg() }
            every { orderItemOptionRepository.saveAll(any<List<OrderItemOption>>()) } answers { firstArg() }

            val items =
                listOf(
                    OrderItemInput(
                        productId = product.id,
                        quantity = 1,
                        price = 3500,
                        selectedOptions = listOf(SelectedOptionSnapshot("사이즈", "라지", 500, 1)),
                    ),
                )
            val order = service.createOrder(booth, 3500, items)
            order.orderNumber shouldBe 1
        }

        test("confirmOrder 정상") {
            val order = Order(orderNumber = 1, totalAmount = 3000, status = OrderStatus.PENDING, booth = booth)
            every { orderRepository.findByIdOrNull(order.id) } returns order

            val confirmed = service.confirmOrder(order.id)
            confirmed.status shouldBe OrderStatus.CONFIRMED
        }

        test("confirmOrder 이미 확인됨 → ORDER_ALREADY_CONFIRMED") {
            val order = Order(orderNumber = 1, totalAmount = 3000, status = OrderStatus.CONFIRMED, booth = booth)
            every { orderRepository.findByIdOrNull(order.id) } returns order

            shouldThrow<CoreException> { service.confirmOrder(order.id) }.type shouldBe ErrorType.ORDER_ALREADY_CONFIRMED
        }

        test("cancelOrder 정상 → 잔액+재고 복구, 트랜잭션, 이벤트") {
            val order = Order(orderNumber = 1, totalAmount = 3000, status = OrderStatus.CONFIRMED, booth = booth).apply { user = buyer }
            val orderItem = OrderItem(quantity = 2, price = 1500, order = order, product = product)

            every { orderRepository.findByIdForUpdate(order.id) } returns order
            every { userRepository.findByIdOrNull(owner.id) } returns owner
            every { userRepository.findByIdForUpdate(buyer.id) } returns buyer
            every { orderItemRepository.findAllByOrderId(order.id) } returns listOf(orderItem)
            every { productRepository.findByIdInForUpdate(listOf(product.id)) } returns listOf(product)
            every { transactionRepository.save(any<Transaction>()) } answers { firstArg() }

            val beforeBalance = buyer.balance
            val beforeStock = product.stock!!

            service.cancelOrder(owner.id, order.id)

            order.status shouldBe OrderStatus.CANCELLED
            buyer.balance shouldBe beforeBalance + 3000
            product.stock shouldBe beforeStock + 2

            verify { transactionRepository.save(any<Transaction>()) }
            verify { eventPublisher.publishEvent(any<OrderCancelledEvent>()) }
        }

        test("cancelOrder ORDER_NOT_CANCELLABLE (PENDING 상태)") {
            val order = Order(orderNumber = 1, totalAmount = 3000, status = OrderStatus.PENDING, booth = booth)
            every { orderRepository.findByIdForUpdate(order.id) } returns order

            shouldThrow<CoreException> { service.cancelOrder(owner.id, order.id) }.type shouldBe ErrorType.ORDER_NOT_CANCELLABLE
        }

        test("cancelOrder ORDER_NOT_FOUND") {
            val id = UUID.randomUUID()
            every { orderRepository.findByIdForUpdate(id) } returns null

            shouldThrow<CoreException> { service.cancelOrder(owner.id, id) }.type shouldBe ErrorType.ORDER_NOT_FOUND
        }

        test("cancelOrder ORDER_CANCEL_FORBIDDEN (소유자도 관리자도 아님)") {
            val otherUser = User(dauthId = "x", name = "제삼자", email = "x@dsm.hs.kr", role = UserRole.STUDENT)
            val order = Order(orderNumber = 1, totalAmount = 3000, status = OrderStatus.CONFIRMED, booth = booth)
            every { orderRepository.findByIdForUpdate(order.id) } returns order
            every { userRepository.findByIdOrNull(otherUser.id) } returns otherUser

            shouldThrow<CoreException> { service.cancelOrder(otherUser.id, order.id) }.type shouldBe ErrorType.ORDER_CANCEL_FORBIDDEN
        }

        test("cancelOrder 관리자는 취소 가능") {
            val order = Order(orderNumber = 1, totalAmount = 3000, status = OrderStatus.CONFIRMED, booth = booth).apply { user = buyer }
            val orderItem = OrderItem(quantity = 1, price = 3000, order = order, product = product)

            every { orderRepository.findByIdForUpdate(order.id) } returns order
            every { userRepository.findByIdOrNull(admin.id) } returns admin
            every { userRepository.findByIdForUpdate(buyer.id) } returns buyer
            every { orderItemRepository.findAllByOrderId(order.id) } returns listOf(orderItem)
            every { productRepository.findByIdInForUpdate(listOf(product.id)) } returns listOf(product)
            every { transactionRepository.save(any<Transaction>()) } answers { firstArg() }

            service.cancelOrder(admin.id, order.id)
            order.status shouldBe OrderStatus.CANCELLED
        }

        test("toOrderResponses 빈 목록") {
            every { orderRepository.findAllByBoothId(booth.id) } returns emptyList()

            service.getOrdersWithItemsByBooth(booth.id) shouldBe emptyList()
        }
    })
