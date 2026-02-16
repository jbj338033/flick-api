package com.flick.storage.db.core.entity

import com.flick.core.enums.TransactionType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "transactions")
class Transaction(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: TransactionType,
    @Column(nullable = false)
    val amount: Int,
    @Column(nullable = false)
    val balanceBefore: Int,
    @Column(nullable = false)
    val balanceAfter: Int,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    val order: Order? = null,
) : BaseEntity()
