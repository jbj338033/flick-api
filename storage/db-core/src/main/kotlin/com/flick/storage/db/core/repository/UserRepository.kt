package com.flick.storage.db.core.repository

import com.flick.storage.db.core.entity.User
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByDauthId(dauthId: String): User?

    fun findByGradeAndRoomAndNumber(
        grade: Int,
        room: Int,
        number: Int,
    ): User?

    fun findByNameContaining(name: String): List<User>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    fun findByIdForUpdate(userId: UUID): User?
}
