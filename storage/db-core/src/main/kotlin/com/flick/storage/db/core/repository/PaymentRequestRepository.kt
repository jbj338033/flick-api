package com.flick.storage.db.core.repository

import com.flick.storage.db.core.entity.PaymentRequest
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import java.util.UUID

interface PaymentRequestRepository : JpaRepository<PaymentRequest, UUID> {
    fun findByCode(code: String): PaymentRequest?

    fun findByOrderId(orderId: UUID): PaymentRequest?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pr FROM PaymentRequest pr WHERE pr.order.id = :orderId")
    fun findByOrderIdForUpdate(orderId: UUID): PaymentRequest?

    fun findAllByIsConfirmedFalseAndExpiresAtBefore(now: LocalDateTime): List<PaymentRequest>
}
