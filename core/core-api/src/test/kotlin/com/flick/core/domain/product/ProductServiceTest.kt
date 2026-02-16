package com.flick.core.domain.product

import com.flick.core.enums.UserRole
import com.flick.core.event.ProductChangedEvent
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Booth
import com.flick.storage.db.core.entity.Product
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.BoothRepository
import com.flick.storage.db.core.repository.OrderItemRepository
import com.flick.storage.db.core.repository.ProductRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID

class ProductServiceTest :
    FunSpec({
        val productRepository = mockk<ProductRepository>()
        val boothRepository = mockk<BoothRepository>()
        val orderItemRepository = mockk<OrderItemRepository>()
        val productOptionService = mockk<ProductOptionService>(relaxed = true)
        val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
        val service = ProductService(productRepository, boothRepository, orderItemRepository, productOptionService, eventPublisher)

        val owner = User(dauthId = "o1", name = "오너", email = "o@dsm.hs.kr", role = UserRole.TEACHER)
        val booth = Booth(name = "부스", user = owner)
        val product = Product(name = "타코야끼", price = 3000, stock = 100, booth = booth)

        test("getProduct 정상") {
            every { productRepository.findByIdOrNull(product.id) } returns product

            service.getProduct(product.id).name shouldBe "타코야끼"
        }

        test("getProduct 존재하지 않음 → PRODUCT_NOT_FOUND") {
            val id = UUID.randomUUID()
            every { productRepository.findByIdOrNull(id) } returns null

            val exception = shouldThrow<CoreException> { service.getProduct(id) }
            exception.type shouldBe ErrorType.PRODUCT_NOT_FOUND
        }

        test("getProductsByBooth 정상") {
            every { productRepository.findAllByBoothId(booth.id) } returns listOf(product)

            service.getProductsByBooth(booth.id).size shouldBe 1
        }

        test("createProduct 정상") {
            every { boothRepository.getReferenceById(booth.id) } returns booth
            every { productRepository.save(any<Product>()) } answers { firstArg() }

            val result = service.createProduct("새상품", 2000, 50, booth.id)
            result.name shouldBe "새상품"
            result.price shouldBe 2000
        }

        test("createProduct 옵션 그룹 포함") {
            every { boothRepository.getReferenceById(booth.id) } returns booth
            every { productRepository.save(any<Product>()) } answers { firstArg() }

            val groups = listOf(OptionGroupInput(name = "맛", options = listOf(OptionInput("매운맛", 500))))
            service.createProduct("상품", 2000, null, booth.id, groups)

            verify { productOptionService.createOptionGroups(any(), eq(groups)) }
        }

        test("updateProduct 정상 + 이벤트 발행") {
            every { productRepository.findByIdOrNull(product.id) } returns product

            val result = service.updateProduct(owner.id, product.id, "수정됨", 4000, null, null, null)
            result.name shouldBe "수정됨"
            result.price shouldBe 4000
            verify { eventPublisher.publishEvent(any<ProductChangedEvent>()) }
        }

        test("updateProduct 소유자 아님 → FORBIDDEN") {
            every { productRepository.findByIdOrNull(product.id) } returns product
            val otherId = UUID.randomUUID()

            val exception = shouldThrow<CoreException> { service.updateProduct(otherId, product.id, "수정", null, null, null, null) }
            exception.type shouldBe ErrorType.FORBIDDEN
        }

        test("deleteProduct 정상 + 이벤트 발행") {
            every { productRepository.findByIdOrNull(product.id) } returns product
            every { orderItemRepository.existsActiveOrdersByProductId(product.id) } returns false
            every { productRepository.delete(product) } returns Unit

            service.deleteProduct(owner.id, product.id)
            verify { productRepository.delete(product) }
            verify { eventPublisher.publishEvent(any<ProductChangedEvent>()) }
        }

        test("deleteProduct 활성 주문 있음 → PRODUCT_HAS_ACTIVE_ORDERS") {
            every { productRepository.findByIdOrNull(product.id) } returns product
            every { orderItemRepository.existsActiveOrdersByProductId(product.id) } returns true

            val exception = shouldThrow<CoreException> { service.deleteProduct(owner.id, product.id) }
            exception.type shouldBe ErrorType.PRODUCT_HAS_ACTIVE_ORDERS
        }
    })
