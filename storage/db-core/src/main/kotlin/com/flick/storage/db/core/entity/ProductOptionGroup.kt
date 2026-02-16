package com.flick.storage.db.core.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "product_option_groups")
class ProductOptionGroup(
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false)
    var isRequired: Boolean = false,
    @Column(nullable = false)
    var maxSelections: Int = 1,
    @Column(nullable = false)
    var sortOrder: Int = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,
) : BaseEntity()
