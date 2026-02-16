package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.CreateNoticeRequest
import com.flick.core.api.controller.v1.response.NoticeResponse
import com.flick.core.api.controller.v1.response.toResponse
import com.flick.core.domain.notice.NoticeService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/notices")
class NoticeController(
    private val noticeService: NoticeService,
) {
    @GetMapping
    fun getNotices(): List<NoticeResponse> = noticeService.getNotices().map { it.toResponse() }

    @GetMapping("/{noticeId}")
    fun getNotice(
        @PathVariable noticeId: UUID,
    ): NoticeResponse = noticeService.getNotice(noticeId).toResponse()

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createNotice(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: CreateNoticeRequest,
    ): NoticeResponse =
        noticeService
            .createNotice(title = request.title, content = request.content, userId = userId)
            .toResponse()

    @DeleteMapping("/{noticeId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteNotice(
        @PathVariable noticeId: UUID,
    ) {
        noticeService.deleteNotice(noticeId)
    }
}
