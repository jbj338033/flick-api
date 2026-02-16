package com.flick.storage.db.core.repository

import com.flick.storage.db.core.entity.FaceEmbedding
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface FaceEmbeddingRepository : JpaRepository<FaceEmbedding, UUID> {
    fun findByUserId(userId: UUID): List<FaceEmbedding>

    fun deleteByUserId(userId: UUID)

    @Query("SELECT fe FROM FaceEmbedding fe JOIN FETCH fe.user")
    fun findAllWithUser(): List<FaceEmbedding>
}
