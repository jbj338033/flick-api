package com.flick.storage.db.core.entity

import com.flick.core.enums.Bank
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "refund_requests")
class RefundRequest(
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val bank: Bank,
    @Column(nullable = false, length = 50)
    val accountNumber: String,
    @Column(nullable = false)
    val amount: Int,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
) : BaseEntity()
