package com.flick.core.domain.payment

import com.flick.core.domain.order.OrderService
import com.flick.core.domain.product.ProductService
import com.flick.core.enums.OrderStatus
import com.flick.core.enums.UserRole
import com.flick.core.event.PaymentCompletedEvent
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Booth
import com.flick.storage.db.core.entity.Order
import com.flick.storage.db.core.entity.OrderItem
import com.flick.storage.db.core.entity.PaymentRequest
import com.flick.storage.db.core.entity.Product
import com.flick.storage.db.core.entity.ProductOption
import com.flick.storage.db.core.entity.ProductOptionGroup
import com.flick.storage.db.core.entity.Transaction
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.BoothRepository
import com.flick.storage.db.core.repository.OrderItemRepository
import com.flick.storage.db.core.repository.PaymentRequestRepository
import com.flick.storage.db.core.repository.ProductOptionGroupRepository
import com.flick.storage.db.core.repository.ProductOptionRepository
import com.flick.storage.db.core.repository.ProductRepository
import com.flick.storage.db.core.repository.TransactionRepository
import com.flick.storage.db.core.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDateTime
import java.util.UUID

class PaymentServiceTest :
    FunSpec({
        val orderService = mockk<OrderService>()
        val productService = mockk<ProductService>()
        val paymentCodeService = mockk<PaymentCodeService>()
        val userRepository = mockk<UserRepository>()
        val boothRepository = mockk<BoothRepository>()
        val productRepository = mockk<ProductRepository>()
        val paymentRequestRepository = mockk<PaymentRequestRepository>()
        val orderItemRepository = mockk<OrderItemRepository>()
        val transactionRepository = mockk<TransactionRepository>()
        val optionGroupRepository = mockk<ProductOptionGroupRepository>()
        val optionRepository = mockk<ProductOptionRepository>()
        val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
        val service =
            PaymentService(
                orderService,
                productService,
                paymentCodeService,
                userRepository,
                boothRepository,
                productRepository,
                paymentRequestRepository,
                orderItemRepository,
                transactionRepository,
                optionGroupRepository,
                optionRepository,
                eventPublisher,
            )

        val owner = User(dauthId = "o1", name = "오너", email = "o@dsm.hs.kr", role = UserRole.TEACHER)
        val buyer = User(dauthId = "b1", name = "구매자", email = "b@dsm.hs.kr", role = UserRole.STUDENT, balance = 50000)
        val booth = Booth(name = "부스", user = owner)
        val product = Product(name = "타코야끼", price = 3000, stock = 100, booth = booth)

        context("createPaymentRequest") {
            test("정상 흐름") {
                val order = Order(orderNumber = 1, totalAmount = 3000, booth = booth)
                every { boothRepository.findByIdOrNull(booth.id) } returns booth
                every { productService.getProduct(product.id) } returns product
                every { optionGroupRepository.findAllByProductIdOrderBySortOrder(product.id) } returns emptyList()
                every { orderService.createOrder(any(), any(), any()) } returns order
                every { paymentCodeService.generateCode(order.id) } returns "123456"
                every { paymentRequestRepository.save(any<PaymentRequest>()) } answers { firstArg() }

                val request = CreatePaymentRequest(booth.id, listOf(PaymentItemInput(product.id, 1)))
                val result = service.createPaymentRequest(request)

                result.code shouldBe "123456"
                result.totalAmount shouldBe 3000
                result.orderNumber shouldBe 1
            }

            test("BOOTH_NOT_FOUND") {
                val id = UUID.randomUUID()
                every { boothRepository.findByIdOrNull(id) } returns null

                shouldThrow<CoreException> {
                    service.createPaymentRequest(CreatePaymentRequest(id, listOf(PaymentItemInput(product.id, 1))))
                }.type shouldBe ErrorType.BOOTH_NOT_FOUND
            }

            test("PRODUCT_NOT_IN_BOOTH") {
                val otherBooth = Booth(name = "다른부스", user = owner)
                val otherProduct = Product(name = "다른상품", price = 2000, booth = otherBooth)
                every { boothRepository.findByIdOrNull(booth.id) } returns booth
                every { productService.getProduct(otherProduct.id) } returns otherProduct

                shouldThrow<CoreException> {
                    service.createPaymentRequest(CreatePaymentRequest(booth.id, listOf(PaymentItemInput(otherProduct.id, 1))))
                }.type shouldBe ErrorType.PRODUCT_NOT_IN_BOOTH
            }

            test("PRODUCT_SOLD_OUT") {
                val soldOutProduct = Product(name = "품절", price = 1000, isSoldOut = true, booth = booth)
                every { boothRepository.findByIdOrNull(booth.id) } returns booth
                every { productService.getProduct(soldOutProduct.id) } returns soldOutProduct

                shouldThrow<CoreException> {
                    service.createPaymentRequest(CreatePaymentRequest(booth.id, listOf(PaymentItemInput(soldOutProduct.id, 1))))
                }.type shouldBe ErrorType.PRODUCT_SOLD_OUT
            }

            test("PRODUCT_STOCK_INSUFFICIENT") {
                val lowStockProduct = Product(name = "재고부족", price = 1000, stock = 1, booth = booth)
                every { boothRepository.findByIdOrNull(booth.id) } returns booth
                every { productService.getProduct(lowStockProduct.id) } returns lowStockProduct

                shouldThrow<CoreException> {
                    service.createPaymentRequest(CreatePaymentRequest(booth.id, listOf(PaymentItemInput(lowStockProduct.id, 5))))
                }.type shouldBe ErrorType.PRODUCT_STOCK_INSUFFICIENT
            }

            test("필수 옵션 누락 → REQUIRED_OPTION_MISSING") {
                val requiredGroup = ProductOptionGroup(name = "사이즈", isRequired = true, maxSelections = 1, sortOrder = 0, product = product)
                every { boothRepository.findByIdOrNull(booth.id) } returns booth
                every { productService.getProduct(product.id) } returns product
                every { optionGroupRepository.findAllByProductIdOrderBySortOrder(product.id) } returns listOf(requiredGroup)

                shouldThrow<CoreException> {
                    service.createPaymentRequest(CreatePaymentRequest(booth.id, listOf(PaymentItemInput(product.id, 1))))
                }.type shouldBe ErrorType.REQUIRED_OPTION_MISSING
            }

            test("옵션 최대 선택 초과 → OPTION_MAX_SELECTIONS_EXCEEDED") {
                val group = ProductOptionGroup(name = "토핑", isRequired = false, maxSelections = 1, sortOrder = 0, product = product)
                val opt1 = ProductOption(name = "치즈", price = 500, optionGroup = group)
                val opt2 = ProductOption(name = "김치", price = 300, optionGroup = group)

                every { boothRepository.findByIdOrNull(booth.id) } returns booth
                every { productService.getProduct(product.id) } returns product
                every { optionRepository.findAllById(listOf(opt1.id, opt2.id)) } returns listOf(opt1, opt2)
                every { optionGroupRepository.findAllByProductIdOrderBySortOrder(product.id) } returns listOf(group)

                shouldThrow<CoreException> {
                    service.createPaymentRequest(
                        CreatePaymentRequest(
                            booth.id,
                            listOf(
                                PaymentItemInput(product.id, 1, listOf(SelectedOptionInput(opt1.id), SelectedOptionInput(opt2.id))),
                            ),
                        ),
                    )
                }.type shouldBe ErrorType.OPTION_MAX_SELECTIONS_EXCEEDED
            }

            test("존재하지 않는 옵션 → PRODUCT_OPTION_NOT_FOUND") {
                val fakeOptionId = UUID.randomUUID()
                every { boothRepository.findByIdOrNull(booth.id) } returns booth
                every { productService.getProduct(product.id) } returns product
                every { optionRepository.findAllById(listOf(fakeOptionId)) } returns emptyList()

                shouldThrow<CoreException> {
                    service.createPaymentRequest(
                        CreatePaymentRequest(booth.id, listOf(PaymentItemInput(product.id, 1, listOf(SelectedOptionInput(fakeOptionId))))),
                    )
                }.type shouldBe ErrorType.PRODUCT_OPTION_NOT_FOUND
            }

            test("다른 상품 옵션 → OPTION_NOT_IN_PRODUCT") {
                val otherProduct = Product(name = "다른상품", price = 2000, booth = booth)
                val otherGroup = ProductOptionGroup(name = "사이즈", product = otherProduct)
                val otherOption = ProductOption(name = "라지", price = 500, optionGroup = otherGroup)

                every { boothRepository.findByIdOrNull(booth.id) } returns booth
                every { productService.getProduct(product.id) } returns product
                every { optionRepository.findAllById(listOf(otherOption.id)) } returns listOf(otherOption)

                shouldThrow<CoreException> {
                    service.createPaymentRequest(
                        CreatePaymentRequest(
                            booth.id,
                            listOf(PaymentItemInput(product.id, 1, listOf(SelectedOptionInput(otherOption.id)))),
                        ),
                    )
                }.type shouldBe ErrorType.OPTION_NOT_IN_PRODUCT
            }

            test("수량 선택 불가 옵션에 quantity>1 → OPTION_QUANTITY_NOT_ALLOWED") {
                val group = ProductOptionGroup(name = "토핑", isRequired = false, maxSelections = 1, sortOrder = 0, product = product)
                val opt = ProductOption(name = "치즈", price = 500, isQuantitySelectable = false, optionGroup = group)

                every { boothRepository.findByIdOrNull(booth.id) } returns booth
                every { productService.getProduct(product.id) } returns product
                every { optionRepository.findAllById(listOf(opt.id)) } returns listOf(opt)
                every { optionGroupRepository.findAllByProductIdOrderBySortOrder(product.id) } returns listOf(group)

                shouldThrow<CoreException> {
                    service.createPaymentRequest(
                        CreatePaymentRequest(
                            booth.id,
                            listOf(PaymentItemInput(product.id, 1, listOf(SelectedOptionInput(opt.id, quantity = 3)))),
                        ),
                    )
                }.type shouldBe ErrorType.OPTION_QUANTITY_NOT_ALLOWED
            }

            test("옵션 가격 총액 반영") {
                val group = ProductOptionGroup(name = "토핑", isRequired = false, maxSelections = 3, sortOrder = 0, product = product)
                val opt = ProductOption(name = "치즈", price = 500, isQuantitySelectable = true, optionGroup = group)
                val order = Order(orderNumber = 1, totalAmount = 4000, booth = booth)

                every { boothRepository.findByIdOrNull(booth.id) } returns booth
                every { productService.getProduct(product.id) } returns product
                every { optionRepository.findAllById(listOf(opt.id)) } returns listOf(opt)
                every { optionGroupRepository.findAllByProductIdOrderBySortOrder(product.id) } returns listOf(group)
                every { orderService.createOrder(any(), eq(4000), any()) } returns order
                every { paymentCodeService.generateCode(order.id) } returns "654321"
                every { paymentRequestRepository.save(any<PaymentRequest>()) } answers { firstArg() }

                val result =
                    service.createPaymentRequest(
                        CreatePaymentRequest(
                            booth.id,
                            listOf(PaymentItemInput(product.id, 1, listOf(SelectedOptionInput(opt.id, quantity = 2)))),
                        ),
                    )
                result.totalAmount shouldBe 4000
            }
        }

        test("getPaymentRequestByCode 정상") {
            val order = Order(orderNumber = 1, totalAmount = 3000, booth = booth)
            val pr = PaymentRequest(code = "111111", order = order, expiresAt = LocalDateTime.now().plusMinutes(3))
            val orderItem = OrderItem(quantity = 1, price = 3000, order = order, product = product)

            every { paymentRequestRepository.findByCode("111111") } returns pr
            every { orderItemRepository.findAllByOrderId(order.id) } returns listOf(orderItem)

            val result = service.getPaymentRequestByCode("111111")
            result.code shouldBe "111111"
            result.totalAmount shouldBe 3000
            result.items.size shouldBe 1
        }

        test("getPaymentRequestByCode PAYMENT_REQUEST_NOT_FOUND") {
            every { paymentRequestRepository.findByCode("000000") } returns null

            shouldThrow<CoreException> {
                service.getPaymentRequestByCode("000000")
            }.type shouldBe ErrorType.PAYMENT_REQUEST_NOT_FOUND
        }

        context("confirmPayment") {
            test("정상 흐름") {
                val order = Order(orderNumber = 1, totalAmount = 3000, status = OrderStatus.PENDING, booth = booth)
                val pr = PaymentRequest(code = "111111", order = order, expiresAt = LocalDateTime.now().plusMinutes(3))
                val orderItem = OrderItem(quantity = 1, price = 3000, order = order, product = product)
                val confirmedOrder = Order(orderNumber = 1, totalAmount = 3000, status = OrderStatus.CONFIRMED, booth = booth)

                every { paymentCodeService.resolveOrderId("111111") } returns order.id
                every { paymentRequestRepository.findByOrderIdForUpdate(order.id) } returns pr
                every { orderService.getOrder(order.id) } returns order
                every { userRepository.findByIdForUpdate(buyer.id) } returns buyer
                every { orderService.getOrderItems(order.id) } returns listOf(orderItem)
                every { productRepository.findByIdInForUpdate(listOf(product.id)) } returns listOf(product)
                every { orderService.confirmOrder(order.id) } returns confirmedOrder
                every { transactionRepository.save(any<Transaction>()) } answers { firstArg() }
                every { orderItemRepository.sumQuantitiesByUserIdAndProductIds(any(), any()) } returns emptyList()

                mockkStatic(TransactionSynchronizationManager::class)
                every { TransactionSynchronizationManager.registerSynchronization(any()) } returns Unit

                val result = service.confirmPayment(buyer.id, "111111")
                result.totalAmount shouldBe 3000
                result.balanceAfter shouldBe 50000 - 3000
                pr.isConfirmed shouldBe true

                verify { eventPublisher.publishEvent(any<PaymentCompletedEvent>()) }
                unmockkStatic(TransactionSynchronizationManager::class)
            }

            test("PAYMENT_CODE_INVALID") {
                every { paymentCodeService.resolveOrderId("bad") } returns null

                shouldThrow<CoreException> {
                    service.confirmPayment(buyer.id, "bad")
                }.type shouldBe ErrorType.PAYMENT_CODE_INVALID
            }

            test("PAYMENT_REQUEST_NOT_FOUND") {
                val orderId = UUID.randomUUID()
                every { paymentCodeService.resolveOrderId("222222") } returns orderId
                every { paymentRequestRepository.findByOrderIdForUpdate(orderId) } returns null

                shouldThrow<CoreException> {
                    service.confirmPayment(buyer.id, "222222")
                }.type shouldBe ErrorType.PAYMENT_REQUEST_NOT_FOUND
            }

            test("ORDER_ALREADY_CONFIRMED") {
                val orderId = UUID.randomUUID()
                val pr = mockk<PaymentRequest>()
                every { pr.isConfirmed } returns true
                every { paymentCodeService.resolveOrderId("333333") } returns orderId
                every { paymentRequestRepository.findByOrderIdForUpdate(orderId) } returns pr

                shouldThrow<CoreException> {
                    service.confirmPayment(buyer.id, "333333")
                }.type shouldBe ErrorType.ORDER_ALREADY_CONFIRMED
            }

            test("PAYMENT_CODE_EXPIRED") {
                val orderId = UUID.randomUUID()
                val pr = mockk<PaymentRequest>()
                every { pr.isConfirmed } returns false
                every { pr.expiresAt } returns LocalDateTime.now().minusMinutes(1)
                every { paymentCodeService.resolveOrderId("444444") } returns orderId
                every { paymentRequestRepository.findByOrderIdForUpdate(orderId) } returns pr

                shouldThrow<CoreException> {
                    service.confirmPayment(buyer.id, "444444")
                }.type shouldBe ErrorType.PAYMENT_CODE_EXPIRED
            }

            test("USER_NOT_FOUND") {
                val order = Order(orderNumber = 1, totalAmount = 3000, booth = booth)
                val pr = PaymentRequest(code = "555555", order = order, expiresAt = LocalDateTime.now().plusMinutes(3))
                val userId = UUID.randomUUID()

                every { paymentCodeService.resolveOrderId("555555") } returns order.id
                every { paymentRequestRepository.findByOrderIdForUpdate(order.id) } returns pr
                every { orderService.getOrder(order.id) } returns order
                every { userRepository.findByIdForUpdate(userId) } returns null

                shouldThrow<CoreException> {
                    service.confirmPayment(userId, "555555")
                }.type shouldBe ErrorType.USER_NOT_FOUND
            }

            test("INSUFFICIENT_BALANCE") {
                val poorBuyer = User(dauthId = "poor", name = "가난", email = "p@dsm.hs.kr", role = UserRole.STUDENT, balance = 100)
                val order = Order(orderNumber = 1, totalAmount = 3000, booth = booth)
                val pr = PaymentRequest(code = "666666", order = order, expiresAt = LocalDateTime.now().plusMinutes(3))
                val orderItem = OrderItem(quantity = 1, price = 3000, order = order, product = product)

                every { paymentCodeService.resolveOrderId("666666") } returns order.id
                every { paymentRequestRepository.findByOrderIdForUpdate(order.id) } returns pr
                every { orderService.getOrder(order.id) } returns order
                every { userRepository.findByIdForUpdate(poorBuyer.id) } returns poorBuyer
                every { orderService.getOrderItems(order.id) } returns listOf(orderItem)
                every { productRepository.findByIdInForUpdate(listOf(product.id)) } returns listOf(product)

                shouldThrow<CoreException> {
                    service.confirmPayment(poorBuyer.id, "666666")
                }.type shouldBe ErrorType.INSUFFICIENT_BALANCE
            }

            test("PURCHASE_LIMIT_EXCEEDED") {
                val limitedProduct = Product(name = "한정", price = 1000, stock = 100, purchaseLimit = 2, booth = booth)
                val order = Order(orderNumber = 1, totalAmount = 1000, booth = booth)
                val pr = PaymentRequest(code = "777777", order = order, expiresAt = LocalDateTime.now().plusMinutes(3))
                val orderItem = OrderItem(quantity = 1, price = 1000, order = order, product = limitedProduct)

                every { paymentCodeService.resolveOrderId("777777") } returns order.id
                every { paymentRequestRepository.findByOrderIdForUpdate(order.id) } returns pr
                every { orderService.getOrder(order.id) } returns order
                every { userRepository.findByIdForUpdate(buyer.id) } returns buyer
                every { orderService.getOrderItems(order.id) } returns listOf(orderItem)
                every { orderItemRepository.sumQuantitiesByUserIdAndProductIds(buyer.id, listOf(limitedProduct.id)) } returns
                    listOf(arrayOf(limitedProduct.id as Any, 2L as Any))

                shouldThrow<CoreException> {
                    service.confirmPayment(buyer.id, "777777")
                }.type shouldBe ErrorType.PURCHASE_LIMIT_EXCEEDED
            }
        }
    })
