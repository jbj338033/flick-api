package com.flick.storage.db.core.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "payment_requests")
class PaymentRequest(
    @Column(nullable = false, length = 6)
    val code: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,
    @Column(nullable = false)
    val expiresAt: LocalDateTime,
    @Column(nullable = false)
    var isConfirmed: Boolean = false,
) : BaseEntity()
