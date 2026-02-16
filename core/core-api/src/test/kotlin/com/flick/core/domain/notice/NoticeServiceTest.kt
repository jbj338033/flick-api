package com.flick.core.domain.notice

import com.flick.core.enums.UserRole
import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.Notice
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.NoticeRepository
import com.flick.storage.db.core.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID

class NoticeServiceTest :
    FunSpec({
        val noticeRepository = mockk<NoticeRepository>()
        val userRepository = mockk<UserRepository>()
        val service = NoticeService(noticeRepository, userRepository)

        val user = User(dauthId = "admin", name = "관리자", email = "admin@dsm.hs.kr", role = UserRole.ADMIN)
        val notice = Notice(title = "공지", content = "내용입니다", user = user)

        test("getNotices 정상") {
            every { noticeRepository.findAllByOrderByCreatedAtDesc() } returns listOf(notice)

            val result = service.getNotices()
            result.size shouldBe 1
            result[0].title shouldBe "공지"
        }

        test("getNotice 정상") {
            every { noticeRepository.findByIdOrNull(notice.id) } returns notice

            service.getNotice(notice.id).title shouldBe "공지"
        }

        test("getNotice 존재하지 않음 → NOTICE_NOT_FOUND") {
            val id = UUID.randomUUID()
            every { noticeRepository.findByIdOrNull(id) } returns null

            val exception = shouldThrow<CoreException> { service.getNotice(id) }
            exception.type shouldBe ErrorType.NOTICE_NOT_FOUND
        }

        test("createNotice 정상") {
            every { userRepository.getReferenceById(user.id) } returns user
            every { noticeRepository.save(any<Notice>()) } answers { firstArg() }

            val result = service.createNotice("새 공지", "새 내용", user.id)
            result.title shouldBe "새 공지"
            result.content shouldBe "새 내용"
        }

        test("deleteNotice 정상") {
            every { noticeRepository.findByIdOrNull(notice.id) } returns notice
            every { noticeRepository.delete(notice) } returns Unit

            service.deleteNotice(notice.id)
            verify { noticeRepository.delete(notice) }
        }
    })
