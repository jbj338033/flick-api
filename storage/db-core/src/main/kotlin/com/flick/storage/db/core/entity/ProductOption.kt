package com.flick.storage.db.core.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "product_options")
class ProductOption(
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false)
    var price: Int,
    @Column(nullable = false)
    var isQuantitySelectable: Boolean = false,
    @Column(nullable = false)
    var sortOrder: Int = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_group_id", nullable = false)
    val optionGroup: ProductOptionGroup,
) : BaseEntity()
