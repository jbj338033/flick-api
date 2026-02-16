package com.flick.storage.db.core.repository

import com.flick.core.enums.TransactionType
import com.flick.storage.db.core.entity.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface TransactionRepository : JpaRepository<Transaction, UUID> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<Transaction>

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = :type")
    fun sumAmountByType(type: TransactionType): Long

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.id = :userId AND t.type = :type")
    fun sumAmountByUserIdAndType(
        userId: UUID,
        type: TransactionType,
    ): Long

    @Query("SELECT t FROM Transaction t JOIN FETCH t.user ORDER BY t.createdAt DESC")
    fun findAllWithUserOrderByCreatedAtDesc(): List<Transaction>
}
