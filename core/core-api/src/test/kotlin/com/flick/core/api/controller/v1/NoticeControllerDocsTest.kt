package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.CreateNoticeRequest
import com.flick.core.domain.notice.NoticeService
import com.flick.storage.db.core.entity.Notice
import com.flick.storage.db.core.entity.User
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.restdocs.ManualRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.util.UUID

@Tag("restdocs")
class NoticeControllerDocsTest :
    FunSpec({
        val restDocumentation = ManualRestDocumentation()
        lateinit var mockMvc: MockMvc
        val objectMapper = jsonMapper { addModule(kotlinModule()) }
        val noticeService = mockk<NoticeService>()
        val userId = UUID.randomUUID()

        val authResolver =
            object : HandlerMethodArgumentResolver {
                override fun supportsParameter(parameter: MethodParameter) = parameter.parameterType == UUID::class.java

                override fun resolveArgument(
                    parameter: MethodParameter,
                    mavContainer: ModelAndViewContainer?,
                    webRequest: NativeWebRequest,
                    binderFactory: WebDataBinderFactory?,
                ) = userId
            }

        beforeEach {
            restDocumentation.beforeTest(NoticeControllerDocsTest::class.java, it.name.name)
            mockMvc =
                MockMvcBuilders
                    .standaloneSetup(NoticeController(noticeService))
                    .setCustomArgumentResolvers(authResolver)
                    .apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
                    .build()
        }

        afterEach {
            restDocumentation.afterTest()
        }

        val noticeFields =
            responseFields(
                fieldWithPath("id").description("공지 ID"),
                fieldWithPath("title").description("제목"),
                fieldWithPath("content").description("내용"),
                fieldWithPath("createdAt").description("생성 시각"),
            )

        val noticeListFields =
            responseFields(
                fieldWithPath("[].id").description("공지 ID"),
                fieldWithPath("[].title").description("제목"),
                fieldWithPath("[].content").description("내용"),
                fieldWithPath("[].createdAt").description("생성 시각"),
            )

        val testUser = User(dauthId = "test", name = "테스터", email = "t@t.com")

        fun createNotice() = Notice(title = "축제 안내", content = "축제가 시작됩니다!", user = testUser)

        test("공지 목록 조회") {
            every { noticeService.getNotices() } returns listOf(createNotice())

            mockMvc
                .get("/api/v1/notices")
                .andExpect { status { isOk() } }
                .andDo { handle(document("notice-list", preprocessResponse(prettyPrint()), noticeListFields)) }
        }

        test("공지 상세 조회") {
            val notice = createNotice()
            every { noticeService.getNotice(notice.id) } returns notice

            mockMvc
                .get("/api/v1/notices/${notice.id}")
                .andExpect { status { isOk() } }
                .andDo { handle(document("notice-detail", preprocessResponse(prettyPrint()), noticeFields)) }
        }

        test("공지 생성") {
            every { noticeService.createNotice(any(), any(), any()) } returns createNotice()

            mockMvc
                .post("/api/v1/notices") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(CreateNoticeRequest("축제 안내", "축제가 시작됩니다!"))
                }.andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "notice-create",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                fieldWithPath("title").description("제목"),
                                fieldWithPath("content").description("내용"),
                            ),
                            noticeFields,
                        ),
                    )
                }
        }

        test("공지 삭제") {
            val notice = createNotice()
            justRun { noticeService.deleteNotice(notice.id) }

            mockMvc
                .delete("/api/v1/notices/${notice.id}")
                .andExpect { status { isOk() } }
                .andDo { handle(document("notice-delete")) }
        }
    })
