package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.CreateRefundRequestRequest
import com.flick.core.domain.refund.RefundRequestService
import com.flick.core.enums.Bank
import com.flick.core.enums.UserRole
import com.flick.storage.db.core.entity.RefundRequest
import com.flick.storage.db.core.entity.User
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.util.UUID

@Tag("restdocs")
class RefundRequestControllerDocsTest :
    FunSpec({
        val restDocumentation = ManualRestDocumentation()
        lateinit var mockMvc: MockMvc
        val objectMapper = jsonMapper { addModule(kotlinModule()) }
        val refundRequestService = mockk<RefundRequestService>()
        val userId = UUID.randomUUID()

        val authResolver =
            object : HandlerMethodArgumentResolver {
                override fun supportsParameter(parameter: MethodParameter) = parameter.parameterType == UUID::class.java

                override fun resolveArgument(
                    parameter: MethodParameter,
                    mavContainer: org.springframework.web.method.support.ModelAndViewContainer?,
                    webRequest: org.springframework.web.context.request.NativeWebRequest,
                    binderFactory: org.springframework.web.bind.support.WebDataBinderFactory?,
                ) = userId
            }

        beforeEach {
            restDocumentation.beforeTest(RefundRequestControllerDocsTest::class.java, it.name.name)
            mockMvc =
                MockMvcBuilders
                    .standaloneSetup(RefundRequestController(refundRequestService))
                    .setCustomArgumentResolvers(authResolver)
                    .apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
                    .build()
        }

        afterEach {
            restDocumentation.afterTest()
        }

        test("환불 요청 생성") {
            val user = User(dauthId = "u1", name = "테스터", email = "t@dsm.hs.kr", role = UserRole.STUDENT, balance = 0)
            val refund = RefundRequest(bank = Bank.KAKAO, accountNumber = "1234567890", amount = 5000, user = user)

            every { refundRequestService.createRefundRequest(userId, Bank.KAKAO, "1234567890") } returns refund

            mockMvc
                .post("/api/v1/refund-requests") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(CreateRefundRequestRequest(Bank.KAKAO, "1234567890"))
                }.andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "refund-request-create",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                fieldWithPath("bank").description("은행"),
                                fieldWithPath("accountNumber").description("계좌번호"),
                            ),
                            responseFields(
                                fieldWithPath("bank").description("은행"),
                                fieldWithPath("amount").description("환불 금액"),
                            ),
                        ),
                    )
                }
        }

        test("환불 요청 목록 조회") {
            val user = User(dauthId = "u1", name = "테스터", email = "t@dsm.hs.kr", role = UserRole.STUDENT)
            val refund = RefundRequest(bank = Bank.KAKAO, accountNumber = "1234567890", amount = 5000, user = user)

            every { refundRequestService.getAllRefundRequests() } returns listOf(refund)

            mockMvc
                .get("/api/v1/refund-requests")
                .andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "refund-request-list",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                fieldWithPath("[].bank").description("은행"),
                                fieldWithPath("[].accountNumber").description("계좌번호 (마스킹)"),
                                fieldWithPath("[].amount").description("환불 금액"),
                            ),
                        ),
                    )
                }
        }
    })
