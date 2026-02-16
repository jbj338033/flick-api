package com.flick.core.domain.notice

import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Notice
import com.flick.storage.db.core.repository.NoticeRepository
import com.flick.storage.db.core.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class NoticeService(
    private val noticeRepository: NoticeRepository,
    private val userRepository: UserRepository,
) {
    fun getNotices(): List<Notice> = noticeRepository.findAllByOrderByCreatedAtDesc()

    fun getNotice(noticeId: UUID): Notice =
        noticeRepository.findByIdOrNull(noticeId)
            ?: throw CoreException(ErrorType.NOTICE_NOT_FOUND)

    @Transactional
    fun createNotice(
        title: String,
        content: String,
        userId: UUID,
    ): Notice {
        val user = userRepository.getReferenceById(userId)
        return noticeRepository.save(Notice(title = title, content = content, user = user))
    }

    @Transactional
    fun deleteNotice(noticeId: UUID) {
        val notice = getNotice(noticeId)
        noticeRepository.delete(notice)
    }
}
