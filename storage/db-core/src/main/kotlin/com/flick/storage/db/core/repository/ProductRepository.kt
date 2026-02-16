package com.flick.storage.db.core.repository

import com.flick.storage.db.core.entity.Product
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ProductRepository : JpaRepository<Product, UUID> {
    fun findAllByBoothId(boothId: UUID): List<Product>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :productIds")
    fun findByIdInForUpdate(productIds: List<UUID>): List<Product>
}
