package com.flick.storage.db.core.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "products")
class Product(
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false)
    var price: Int,
    var stock: Int? = null,
    @Column(nullable = false)
    var isSoldOut: Boolean = false,
    var purchaseLimit: Int? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_id", nullable = false)
    val booth: Booth,
) : BaseEntity()
