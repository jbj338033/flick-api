package com.flick.core.api.controller.v1

import com.flick.core.domain.notification.NotificationService
import com.flick.core.enums.NotificationType
import com.flick.storage.db.core.entity.Notification
import com.flick.storage.db.core.entity.User
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.springframework.core.MethodParameter
import org.springframework.restdocs.ManualRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.util.UUID

@Tag("restdocs")
class NotificationControllerDocsTest :
    FunSpec({
        val restDocumentation = ManualRestDocumentation()
        lateinit var mockMvc: MockMvc
        val notificationService = mockk<NotificationService>()
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
            restDocumentation.beforeTest(NotificationControllerDocsTest::class.java, it.name.name)
            mockMvc =
                MockMvcBuilders
                    .standaloneSetup(NotificationController(notificationService))
                    .setCustomArgumentResolvers(authResolver)
                    .apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
                    .build()
        }

        afterEach {
            restDocumentation.afterTest()
        }

        test("알림 목록 조회") {
            val testUser = User(dauthId = "test", name = "테스터", email = "t@t.com")
            val notification =
                Notification(
                    type = NotificationType.PAYMENT_COMPLETED,
                    title = "결제 완료",
                    body = "떡볶이 외 1건 결제가 완료되었습니다",
                    user = testUser,
                )
            every { notificationService.getNotifications(userId) } returns listOf(notification)

            mockMvc
                .get("/api/v1/notifications")
                .andExpect { status { isOk() } }
                .andDo {
                    handle(
                        document(
                            "notification-list",
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                fieldWithPath("[].id").description("알림 ID"),
                                fieldWithPath("[].type").description("알림 유형 (PAYMENT_COMPLETED, POINT_CHARGED, NOTICE_CREATED)"),
                                fieldWithPath("[].title").description("제목"),
                                fieldWithPath("[].body").description("내용"),
                                fieldWithPath("[].isRead").description("읽음 여부"),
                                fieldWithPath("[].createdAt").description("생성 시각"),
                            ),
                        ),
                    )
                }
        }

        test("읽지 않은 알림 수 조회") {
            every { notificationService.getUnreadCount(userId) } returns 3L

            mockMvc
                .get("/api/v1/notifications/unread-count")
                .andExpect { status { isOk() } }
                .andDo {
                    handle(
                        document(
                            "notification-unread-count",
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                fieldWithPath("count").description("읽지 않은 알림 수"),
                            ),
                        ),
                    )
                }
        }

        test("알림 읽음 처리") {
            val notificationId = UUID.randomUUID()
            justRun { notificationService.markAsRead(userId, notificationId) }

            mockMvc
                .post("/api/v1/notifications/$notificationId/read")
                .andExpect { status { isOk() } }
                .andDo { handle(document("notification-read")) }
        }

        test("전체 알림 읽음 처리") {
            justRun { notificationService.markAllAsRead(userId) }

            mockMvc
                .post("/api/v1/notifications/read-all")
                .andExpect { status { isOk() } }
                .andDo { handle(document("notification-read-all")) }
        }
    })
