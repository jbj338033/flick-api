package com.flick.core.domain.product

import com.flick.core.event.ProductChangedEvent
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Product
import com.flick.storage.db.core.repository.BoothRepository
import com.flick.storage.db.core.repository.OrderItemRepository
import com.flick.storage.db.core.repository.ProductRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ProductService(
    private val productRepository: ProductRepository,
    private val boothRepository: BoothRepository,
    private val orderItemRepository: OrderItemRepository,
    private val productOptionService: ProductOptionService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    fun getProduct(productId: UUID): Product =
        productRepository.findByIdOrNull(productId)
            ?: throw CoreException(ErrorType.PRODUCT_NOT_FOUND)

    fun getProductsByBooth(boothId: UUID): List<Product> = productRepository.findAllByBoothId(boothId)

    @Transactional
    fun createProduct(
        name: String,
        price: Int,
        stock: Int?,
        boothId: UUID,
        optionGroups: List<OptionGroupInput> = emptyList(),
    ): Product {
        val booth = boothRepository.getReferenceById(boothId)
        val product = productRepository.save(Product(name = name, price = price, stock = stock, booth = booth))
        if (optionGroups.isNotEmpty()) {
            productOptionService.createOptionGroups(product, optionGroups)
        }
        return product
    }

    @Transactional
    fun updateProduct(
        userId: UUID,
        productId: UUID,
        name: String?,
        price: Int?,
        stock: Int?,
        purchaseLimit: Int?,
        isSoldOut: Boolean?,
    ): Product {
        val product = getProduct(productId)
        verifyOwner(product, userId)
        name?.let { product.name = it }
        price?.let { product.price = it }
        stock?.let { product.stock = it }
        purchaseLimit?.let { product.purchaseLimit = it }
        isSoldOut?.let { product.isSoldOut = it }
        eventPublisher.publishEvent(ProductChangedEvent(product.booth.id))
        return product
    }

    @Transactional
    fun deleteProduct(
        userId: UUID,
        productId: UUID,
    ) {
        val product = getProduct(productId)
        verifyOwner(product, userId)
        if (orderItemRepository.existsActiveOrdersByProductId(productId)) {
            throw CoreException(ErrorType.PRODUCT_HAS_ACTIVE_ORDERS)
        }
        val boothId = product.booth.id
        productRepository.delete(product)
        eventPublisher.publishEvent(ProductChangedEvent(boothId))
    }

    private fun verifyOwner(
        product: Product,
        userId: UUID,
    ) {
        if (product.booth.user.id != userId) throw CoreException(ErrorType.FORBIDDEN)
    }
}
