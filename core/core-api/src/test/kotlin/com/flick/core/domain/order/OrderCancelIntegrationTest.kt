package com.flick.core.domain.order

import com.flick.core.domain.payment.CreatePaymentRequest
import com.flick.core.domain.payment.PaymentItemInput
import com.flick.core.domain.payment.PaymentService
import com.flick.core.enums.OrderStatus
import com.flick.core.enums.TransactionType
import com.flick.core.enums.UserRole
import com.flick.core.support.IntegrationTest
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Booth
import com.flick.storage.db.core.entity.Product
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.BoothRepository
import com.flick.storage.db.core.repository.OrderRepository
import com.flick.storage.db.core.repository.ProductRepository
import com.flick.storage.db.core.repository.TransactionRepository
import com.flick.storage.db.core.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

@IntegrationTest
class OrderCancelIntegrationTest : FunSpec() {
    @Autowired
    lateinit var orderService: OrderService

    @Autowired
    lateinit var paymentService: PaymentService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var boothRepository: BoothRepository

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var transactionRepository: TransactionRepository

    init {
        test("주문 취소 → 잔액 복구 + 재고 복구") {
            val owner =
                userRepository.save(
                    User(dauthId = "cancel-test-owner", name = "총괄", email = "o@t.com", role = UserRole.STUDENT, balance = 0),
                )
            val buyer =
                userRepository.save(
                    User(dauthId = "cancel-test-buyer", name = "구매자", email = "b@t.com", role = UserRole.STUDENT, balance = 10000),
                )
            val booth = boothRepository.save(Booth(name = "취소 부스", user = owner))
            val product = productRepository.save(Product(name = "취소상품", price = 3000, stock = 10, booth = booth))

            val req =
                paymentService.createPaymentRequest(
                    CreatePaymentRequest(boothId = booth.id, items = listOf(PaymentItemInput(product.id, 2))),
                )
            paymentService.confirmPayment(code = req.code, userId = buyer.id)

            userRepository.findByIdOrNull(buyer.id)!!.balance shouldBe 4000
            productRepository.findByIdOrNull(product.id)!!.stock shouldBe 8

            orderService.cancelOrder(orderId = req.orderId, userId = owner.id)

            val updatedBuyer = userRepository.findByIdOrNull(buyer.id)!!
            updatedBuyer.balance shouldBe 10000

            val updatedProduct = productRepository.findByIdOrNull(product.id)!!
            updatedProduct.stock shouldBe 10

            val order = orderRepository.findByIdOrNull(req.orderId)!!
            order.status shouldBe OrderStatus.CANCELLED

            val transactions = transactionRepository.findAllByUserIdOrderByCreatedAtDesc(buyer.id)
            transactions.any { it.type == TransactionType.REFUND } shouldBe true
        }

        test("CONFIRMED가 아닌 주문 취소 시 ORDER_NOT_CANCELLABLE") {
            val user =
                userRepository.save(
                    User(dauthId = "cancel-test-2", name = "테스터", email = "ct@t.com", role = UserRole.STUDENT, balance = 10000),
                )
            val booth = boothRepository.save(Booth(name = "취소 부스2", user = user))
            val product = productRepository.save(Product(name = "취소상품2", price = 1000, stock = 5, booth = booth))

            val req =
                paymentService.createPaymentRequest(
                    CreatePaymentRequest(boothId = booth.id, items = listOf(PaymentItemInput(product.id, 1))),
                )

            val exception =
                shouldThrow<CoreException> {
                    orderService.cancelOrder(orderId = req.orderId, userId = user.id)
                }
            exception.type shouldBe ErrorType.ORDER_NOT_CANCELLABLE
        }
    }
}
