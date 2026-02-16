package com.flick.core.domain.product

import com.flick.core.enums.UserRole
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Booth
import com.flick.storage.db.core.entity.Product
import com.flick.storage.db.core.entity.ProductOption
import com.flick.storage.db.core.entity.ProductOptionGroup
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.ProductOptionGroupRepository
import com.flick.storage.db.core.repository.ProductOptionRepository
import com.flick.storage.db.core.repository.ProductRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID

class ProductOptionServiceTest :
    FunSpec({
        val productRepository = mockk<ProductRepository>()
        val optionGroupRepository = mockk<ProductOptionGroupRepository>()
        val optionRepository = mockk<ProductOptionRepository>()
        val service = ProductOptionService(productRepository, optionGroupRepository, optionRepository)

        val owner = User(dauthId = "o1", name = "오너", email = "o@dsm.hs.kr", role = UserRole.TEACHER)
        val booth = Booth(name = "부스", user = owner)
        val product = Product(name = "상품", price = 3000, booth = booth)
        val group = ProductOptionGroup(name = "사이즈", isRequired = true, maxSelections = 1, sortOrder = 0, product = product)
        val option = ProductOption(name = "라지", price = 500, isQuantitySelectable = false, sortOrder = 0, optionGroup = group)

        test("getOptionGroupsByProduct 정상") {
            every { optionGroupRepository.findAllByProductIdOrderBySortOrder(product.id) } returns listOf(group)
            every { optionRepository.findAllByOptionGroupIdInOrderBySortOrder(listOf(group.id)) } returns listOf(option)

            val result = service.getOptionGroupsByProduct(product.id)
            result.size shouldBe 1
            result[0].name shouldBe "사이즈"
            result[0].options.size shouldBe 1
        }

        test("getOptionGroupsByProduct 빈 목록") {
            every { optionGroupRepository.findAllByProductIdOrderBySortOrder(product.id) } returns emptyList()

            service.getOptionGroupsByProduct(product.id).shouldBeEmpty()
        }

        test("getOptionGroupResponsesByProductIds 빈 productIds") {
            service.getOptionGroupResponsesByProductIds(emptyList()) shouldBe emptyMap()
        }

        test("getOptionGroupResponsesByProductIds 정상") {
            every { optionGroupRepository.findAllByProductIdInOrderBySortOrder(listOf(product.id)) } returns listOf(group)
            every { optionRepository.findAllByOptionGroupIdInOrderBySortOrder(listOf(group.id)) } returns listOf(option)

            val result = service.getOptionGroupResponsesByProductIds(listOf(product.id))
            result.size shouldBe 1
            result[product.id]!!.size shouldBe 1
        }

        test("createOptionGroup 정상") {
            every { productRepository.findByIdOrNull(product.id) } returns product
            every { optionGroupRepository.save(any<ProductOptionGroup>()) } answers { firstArg() }
            every { optionRepository.save(any<ProductOption>()) } answers { firstArg() }

            val input = OptionGroupInput("토핑", true, 2, 1, listOf(OptionInput("치즈", 300)))
            val result = service.createOptionGroup(owner.id, product.id, input)
            result.name shouldBe "토핑"
        }

        test("createOptionGroup PRODUCT_NOT_FOUND") {
            val id = UUID.randomUUID()
            every { productRepository.findByIdOrNull(id) } returns null

            shouldThrow<CoreException> {
                service.createOptionGroup(owner.id, id, OptionGroupInput("토핑"))
            }.type shouldBe ErrorType.PRODUCT_NOT_FOUND
        }

        test("createOptionGroup FORBIDDEN") {
            every { productRepository.findByIdOrNull(product.id) } returns product
            val otherId = UUID.randomUUID()

            shouldThrow<CoreException> {
                service.createOptionGroup(otherId, product.id, OptionGroupInput("토핑"))
            }.type shouldBe ErrorType.FORBIDDEN
        }

        test("updateOptionGroup 정상") {
            every { optionGroupRepository.findByIdOrNull(group.id) } returns group
            every { optionRepository.findAllByOptionGroupIdInOrderBySortOrder(listOf(group.id)) } returns listOf(option)

            val result = service.updateOptionGroup(owner.id, group.id, "수정됨", null, 3, null)
            result.name shouldBe "수정됨"
            result.maxSelections shouldBe 3
        }

        test("updateOptionGroup OPTION_GROUP_NOT_FOUND") {
            val id = UUID.randomUUID()
            every { optionGroupRepository.findByIdOrNull(id) } returns null

            shouldThrow<CoreException> {
                service.updateOptionGroup(owner.id, id, "수정", null, null, null)
            }.type shouldBe ErrorType.OPTION_GROUP_NOT_FOUND
        }

        test("deleteOptionGroup 정상") {
            every { optionGroupRepository.findByIdOrNull(group.id) } returns group
            every { optionGroupRepository.delete(group) } returns Unit

            service.deleteOptionGroup(owner.id, group.id)
            verify { optionGroupRepository.delete(group) }
        }

        test("deleteOptionGroup FORBIDDEN") {
            every { optionGroupRepository.findByIdOrNull(group.id) } returns group
            val otherId = UUID.randomUUID()

            shouldThrow<CoreException> {
                service.deleteOptionGroup(otherId, group.id)
            }.type shouldBe ErrorType.FORBIDDEN
        }

        test("createOption 정상") {
            every { optionGroupRepository.findByIdOrNull(group.id) } returns group
            every { optionRepository.save(any<ProductOption>()) } answers { firstArg() }

            val result = service.createOption(owner.id, group.id, "스몰", 0, false, 1)
            result.name shouldBe "스몰"
        }

        test("updateOption 정상") {
            every { optionRepository.findByIdOrNull(option.id) } returns option

            val result = service.updateOption(owner.id, option.id, "엑스라지", 1000, null, null)
            result.name shouldBe "엑스라지"
            result.price shouldBe 1000
        }

        test("updateOption PRODUCT_OPTION_NOT_FOUND") {
            val id = UUID.randomUUID()
            every { optionRepository.findByIdOrNull(id) } returns null

            shouldThrow<CoreException> {
                service.updateOption(owner.id, id, "수정", null, null, null)
            }.type shouldBe ErrorType.PRODUCT_OPTION_NOT_FOUND
        }

        test("updateOption FORBIDDEN") {
            every { optionRepository.findByIdOrNull(option.id) } returns option
            val otherId = UUID.randomUUID()

            shouldThrow<CoreException> {
                service.updateOption(otherId, option.id, "수정", null, null, null)
            }.type shouldBe ErrorType.FORBIDDEN
        }

        test("deleteOption 정상") {
            every { optionRepository.findByIdOrNull(option.id) } returns option
            every { optionRepository.delete(option) } returns Unit

            service.deleteOption(owner.id, option.id)
            verify { optionRepository.delete(option) }
        }

        test("deleteOption FORBIDDEN") {
            every { optionRepository.findByIdOrNull(option.id) } returns option
            val otherId = UUID.randomUUID()

            shouldThrow<CoreException> {
                service.deleteOption(otherId, option.id)
            }.type shouldBe ErrorType.FORBIDDEN
        }
    })
