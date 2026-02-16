package com.flick.storage.db.core.repository

import com.flick.storage.db.core.entity.ProductOption
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ProductOptionRepository : JpaRepository<ProductOption, UUID> {
    @Query("SELECT o FROM ProductOption o WHERE o.optionGroup.id IN :groupIds ORDER BY o.sortOrder")
    fun findAllByOptionGroupIdInOrderBySortOrder(groupIds: List<UUID>): List<ProductOption>
}
