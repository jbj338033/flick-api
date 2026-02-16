package com.flick.storage.db.core.repository

import com.flick.storage.db.core.entity.Booth
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BoothRepository : JpaRepository<Booth, UUID> {
    fun findByUserId(userId: UUID): Booth?

    fun existsByUserId(userId: UUID): Boolean

    fun findByPairingCode(pairingCode: String): Booth?
}
