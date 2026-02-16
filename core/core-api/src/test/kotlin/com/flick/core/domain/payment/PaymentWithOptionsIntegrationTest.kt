package com.flick.core.domain.payment

import com.flick.core.enums.OrderStatus
import com.flick.core.enums.UserRole
import com.flick.core.support.IntegrationTest
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Booth
import com.flick.storage.db.core.entity.Product
import com.flick.storage.db.core.entity.ProductOption
import com.flick.storage.db.core.entity.ProductOptionGroup
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.BoothRepository
import com.flick.storage.db.core.repository.OrderItemOptionRepository
import com.flick.storage.db.core.repository.OrderItemRepository
import com.flick.storage.db.core.repository.OrderRepository
import com.flick.storage.db.core.repository.ProductOptionGroupRepository
import com.flick.storage.db.core.repository.ProductOptionRepository
import com.flick.storage.db.core.repository.ProductRepository
import com.flick.storage.db.core.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

@IntegrationTest
class PaymentWithOptionsIntegrationTest : FunSpec() {
    @Autowired
    lateinit var paymentService: PaymentService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var boothRepository: BoothRepository

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var optionGroupRepository: ProductOptionGroupRepository

    @Autowired
    lateinit var optionRepository: ProductOptionRepository

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var orderItemRepository: OrderItemRepository

    @Autowired
    lateinit var orderItemOptionRepository: OrderItemOptionRepository

    init {
        test("옵션 포함 결제 요청 → totalAmount에 옵션 가격 반영") {
            val user =
                userRepository.save(
                    User(dauthId = "opt-test-1", name = "옵션테스터", email = "opt1@t.com", role = UserRole.STUDENT, balance = 20000),
                )
            val booth = boothRepository.save(Booth(name = "옵션 부스", user = user))
            val product = productRepository.save(Product(name = "커피", price = 2000, stock = 10, booth = booth))
            val group =
                optionGroupRepository.save(
                    ProductOptionGroup(name = "사이즈", isRequired = false, maxSelections = 1, sortOrder = 0, product = product),
                )
            val option =
                optionRepository.save(
                    ProductOption(name = "라지", price = 500, isQuantitySelectable = false, sortOrder = 0, optionGroup = group),
                )

            val result =
                paymentService.createPaymentRequest(
                    CreatePaymentRequest(
                        boothId = booth.id,
                        items = listOf(PaymentItemInput(product.id, 1, listOf(SelectedOptionInput(option.id)))),
                    ),
                )

            result.totalAmount shouldBe 2500
        }

        test("필수 옵션 누락 시 REQUIRED_OPTION_MISSING") {
            val user =
                userRepository.save(
                    User(dauthId = "opt-test-2", name = "필수테스터", email = "opt2@t.com", role = UserRole.STUDENT, balance = 20000),
                )
            val booth = boothRepository.save(Booth(name = "필수옵션 부스", user = user))
            val product = productRepository.save(Product(name = "커피2", price = 2000, stock = 10, booth = booth))
            optionGroupRepository.save(
                ProductOptionGroup(name = "사이즈", isRequired = true, maxSelections = 1, sortOrder = 0, product = product),
            )

            val exception =
                shouldThrow<CoreException> {
                    paymentService.createPaymentRequest(
                        CreatePaymentRequest(boothId = booth.id, items = listOf(PaymentItemInput(product.id, 1))),
                    )
                }
            exception.type shouldBe ErrorType.REQUIRED_OPTION_MISSING
        }

        test("옵션 포함 결제 확인 → 잔액 차감 + OrderItemOption 저장") {
            val owner =
                userRepository.save(
                    User(dauthId = "opt-test-3-owner", name = "주인", email = "optO@t.com", role = UserRole.STUDENT, balance = 0),
                )
            val buyer =
                userRepository.save(
                    User(dauthId = "opt-test-3-buyer", name = "구매자", email = "optB@t.com", role = UserRole.STUDENT, balance = 20000),
                )
            val booth = boothRepository.save(Booth(name = "옵션확인 부스", user = owner))
            val product = productRepository.save(Product(name = "커피3", price = 2000, stock = 10, booth = booth))
            val group =
                optionGroupRepository.save(
                    ProductOptionGroup(name = "토핑", isRequired = false, maxSelections = 2, sortOrder = 0, product = product),
                )
            val opt =
                optionRepository.save(
                    ProductOption(name = "치즈", price = 300, isQuantitySelectable = false, sortOrder = 0, optionGroup = group),
                )

            val request =
                paymentService.createPaymentRequest(
                    CreatePaymentRequest(
                        boothId = booth.id,
                        items = listOf(PaymentItemInput(product.id, 1, listOf(SelectedOptionInput(opt.id)))),
                    ),
                )

            val confirmResult = paymentService.confirmPayment(code = request.code, userId = buyer.id)
            confirmResult.totalAmount shouldBe 2300
            confirmResult.balanceAfter shouldBe 20000 - 2300

            val order = orderRepository.findByIdOrNull(request.orderId)!!
            order.status shouldBe OrderStatus.CONFIRMED

            val items = orderItemRepository.findAllByOrderId(order.id)
            items.size shouldBe 1

            val options = orderItemOptionRepository.findAllByOrderItemIdIn(items.map { it.id })
            options.size shouldBe 1
            options[0].name shouldBe "치즈"
            options[0].price shouldBe 300
        }
    }
}
