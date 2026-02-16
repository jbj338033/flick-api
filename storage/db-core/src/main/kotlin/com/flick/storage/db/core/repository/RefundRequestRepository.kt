package com.flick.storage.db.core.repository

import com.flick.storage.db.core.entity.RefundRequest
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RefundRequestRepository : JpaRepository<RefundRequest, UUID> {
    fun existsByUserId(userId: UUID): Boolean

    fun findAllByOrderByCreatedAtDesc(): List<RefundRequest>
}
