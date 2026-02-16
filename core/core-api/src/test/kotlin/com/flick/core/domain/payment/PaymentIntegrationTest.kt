package com.flick.core.domain.payment

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
class PaymentIntegrationTest : FunSpec() {
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
        test("결제 요청 → 코드 발급 검증") {
            val user =
                userRepository.save(
                    User(dauthId = "pay-test-1", name = "테스터", email = "t@t.com", role = UserRole.STUDENT, balance = 10000),
                )
            val booth = boothRepository.save(Booth(name = "테스트 부스", user = user))
            val product = productRepository.save(Product(name = "떡볶이", price = 3000, stock = 10, booth = booth))

            val result =
                paymentService.createPaymentRequest(
                    CreatePaymentRequest(boothId = booth.id, items = listOf(PaymentItemInput(product.id, 2))),
                )

            result.code.length shouldBe 6
            result.totalAmount shouldBe 6000
            result.orderNumber shouldBe 1
        }

        test("결제 확인 → 잔액 차감, 재고 차감, 주문 CONFIRMED") {
            val user =
                userRepository.save(
                    User(dauthId = "pay-test-2", name = "테스터2", email = "t2@t.com", role = UserRole.STUDENT, balance = 10000),
                )
            val booth = boothRepository.save(Booth(name = "테스트 부스2", user = user))
            val product = productRepository.save(Product(name = "순대", price = 4000, stock = 5, booth = booth))

            val request =
                paymentService.createPaymentRequest(
                    CreatePaymentRequest(boothId = booth.id, items = listOf(PaymentItemInput(product.id, 1))),
                )
            val confirmResult = paymentService.confirmPayment(code = request.code, userId = user.id)

            confirmResult.totalAmount shouldBe 4000
            confirmResult.balanceAfter shouldBe 6000

            val updatedUser = userRepository.findByIdOrNull(user.id)!!
            updatedUser.balance shouldBe 6000

            val updatedProduct = productRepository.findByIdOrNull(product.id)!!
            updatedProduct.stock shouldBe 4

            val order = orderRepository.findByIdOrNull(request.orderId)!!
            order.status shouldBe OrderStatus.CONFIRMED

            val transactions = transactionRepository.findAllByUserIdOrderByCreatedAtDesc(user.id)
            transactions.size shouldBe 1
            transactions[0].type shouldBe TransactionType.PAYMENT
            transactions[0].amount shouldBe 4000
        }

        test("잔액 부족 시 INSUFFICIENT_BALANCE") {
            val user =
                userRepository.save(
                    User(dauthId = "pay-test-3", name = "테스터3", email = "t3@t.com", role = UserRole.STUDENT, balance = 1000),
                )
            val booth = boothRepository.save(Booth(name = "테스트 부스3", user = user))
            val product = productRepository.save(Product(name = "비싼것", price = 5000, stock = 10, booth = booth))

            val request =
                paymentService.createPaymentRequest(
                    CreatePaymentRequest(boothId = booth.id, items = listOf(PaymentItemInput(product.id, 1))),
                )

            val exception =
                shouldThrow<CoreException> {
                    paymentService.confirmPayment(code = request.code, userId = user.id)
                }
            exception.type shouldBe ErrorType.INSUFFICIENT_BALANCE
        }

        test("이미 확인된 결제 시 ORDER_ALREADY_CONFIRMED") {
            val user =
                userRepository.save(
                    User(dauthId = "pay-test-4", name = "테스터4", email = "t4@t.com", role = UserRole.STUDENT, balance = 20000),
                )
            val booth = boothRepository.save(Booth(name = "테스트 부스4", user = user))
            val product = productRepository.save(Product(name = "음료", price = 2000, stock = 10, booth = booth))

            val request =
                paymentService.createPaymentRequest(
                    CreatePaymentRequest(boothId = booth.id, items = listOf(PaymentItemInput(product.id, 1))),
                )
            paymentService.confirmPayment(code = request.code, userId = user.id)

            val exception =
                shouldThrow<CoreException> {
                    paymentService.confirmPayment(code = request.code, userId = user.id)
                }
            exception.type shouldBe ErrorType.PAYMENT_CODE_INVALID
        }

        test("구매 제한 초과 시 PURCHASE_LIMIT_EXCEEDED") {
            val owner =
                userRepository.save(
                    User(dauthId = "pay-test-5-owner", name = "주인", email = "o@t.com", role = UserRole.STUDENT, balance = 0),
                )
            val buyer =
                userRepository.save(
                    User(dauthId = "pay-test-5-buyer", name = "구매자", email = "b@t.com", role = UserRole.STUDENT, balance = 50000),
                )
            val booth = boothRepository.save(Booth(name = "제한 부스", user = owner))
            val product =
                productRepository.save(
                    Product(name = "한정 음료", price = 1000, stock = 100, booth = booth).apply { purchaseLimit = 2 },
                )

            val req1 =
                paymentService.createPaymentRequest(
                    CreatePaymentRequest(boothId = booth.id, items = listOf(PaymentItemInput(product.id, 2))),
                )
            paymentService.confirmPayment(code = req1.code, userId = buyer.id)

            val req2 =
                paymentService.createPaymentRequest(
                    CreatePaymentRequest(boothId = booth.id, items = listOf(PaymentItemInput(product.id, 1))),
                )
            val exception =
                shouldThrow<CoreException> {
                    paymentService.confirmPayment(code = req2.code, userId = buyer.id)
                }
            exception.type shouldBe ErrorType.PURCHASE_LIMIT_EXCEEDED
        }

        test("결제 요청 코드로 주문 상세 조회") {
            val user =
                userRepository.save(
                    User(dauthId = "pay-test-6", name = "조회테스터", email = "q@t.com", role = UserRole.STUDENT, balance = 10000),
                )
            val booth = boothRepository.save(Booth(name = "조회 부스", user = user))
            val product = productRepository.save(Product(name = "조회상품", price = 2000, stock = 10, booth = booth))

            val request =
                paymentService.createPaymentRequest(
                    CreatePaymentRequest(boothId = booth.id, items = listOf(PaymentItemInput(product.id, 1))),
                )
            val detail = paymentService.getPaymentRequestByCode(request.code)

            detail.orderId shouldBe request.orderId
            detail.boothName shouldBe "조회 부스"
            detail.confirmed shouldBe false
            detail.expired shouldBe false
            detail.items.size shouldBe 1
            detail.items[0].productName shouldBe "조회상품"
        }
    }
}
