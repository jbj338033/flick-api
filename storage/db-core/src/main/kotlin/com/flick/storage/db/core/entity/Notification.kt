package com.flick.storage.db.core.entity

import com.flick.core.enums.NotificationType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "notifications")
class Notification(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: NotificationType,
    @Column(nullable = false)
    val title: String,
    @Column(nullable = false)
    val body: String,
    @Column(nullable = false)
    var isRead: Boolean = false,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
) : BaseEntity()
