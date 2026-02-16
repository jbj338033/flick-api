package com.flick.storage.db.core.entity

import com.flick.core.enums.UserRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User(
    @Column(nullable = false, unique = true)
    val dauthId: String,
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false)
    var email: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.STUDENT,
    @Column(nullable = false)
    var balance: Int = 0,
    var grade: Int? = null,
    var room: Int? = null,
    var number: Int? = null,
    @Column(nullable = false)
    var isGrantClaimed: Boolean = false,
) : BaseEntity() {
    fun charge(amount: Int) {
        require(amount > 0) { "Charge amount must be positive" }
        balance += amount
    }

    fun pay(amount: Int) {
        require(amount > 0) { "Payment amount must be positive" }
        check(balance >= amount) { "Insufficient balance: $balance < $amount" }
        balance -= amount
    }

    fun claimGrant() {
        check(!isGrantClaimed) { "Grant already claimed" }
        balance += INITIAL_BALANCE
        isGrantClaimed = true
    }

    companion object {
        const val INITIAL_BALANCE = 1000
    }
}
