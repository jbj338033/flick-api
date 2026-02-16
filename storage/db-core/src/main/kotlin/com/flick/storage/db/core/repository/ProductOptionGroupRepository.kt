package com.flick.storage.db.core.repository

import com.flick.storage.db.core.entity.ProductOptionGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ProductOptionGroupRepository : JpaRepository<ProductOptionGroup, UUID> {
    fun findAllByProductIdOrderBySortOrder(productId: UUID): List<ProductOptionGroup>

    @Query("SELECT g FROM ProductOptionGroup g WHERE g.product.id IN :productIds ORDER BY g.sortOrder")
    fun findAllByProductIdInOrderBySortOrder(productIds: List<UUID>): List<ProductOptionGroup>
}
