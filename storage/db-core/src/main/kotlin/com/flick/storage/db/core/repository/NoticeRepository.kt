package com.flick.storage.db.core.repository

import com.flick.storage.db.core.entity.Notice
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NoticeRepository : JpaRepository<Notice, UUID> {
    fun findAllByOrderByCreatedAtDesc(): List<Notice>
}
