package com.flick.storage.db.core.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "order_item_options")
class OrderItemOption(
    @Column(nullable = false)
    val groupName: String,
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false)
    val price: Int,
    @Column(nullable = false)
    val quantity: Int = 1,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    val orderItem: OrderItem,
) : BaseEntity()
