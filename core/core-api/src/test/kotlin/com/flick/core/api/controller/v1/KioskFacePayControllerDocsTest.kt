package com.flick.core.api.controller.v1

import com.flick.core.domain.facepay.FaceCandidate
import com.flick.core.domain.facepay.FacePayService
import com.flick.core.domain.facepay.RecognitionResult
import com.flick.core.domain.payment.PaymentConfirmResult
import com.flick.core.domain.payment.PaymentService
import com.flick.core.enums.FaceRecognitionStatus
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Tag
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
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.util.UUID

@Tag("restdocs")
class KioskFacePayControllerDocsTest :
    FunSpec({
        val restDocumentation = ManualRestDocumentation()
        lateinit var mockMvc: MockMvc
        val objectMapper = jsonMapper { addModule(kotlinModule()) }
        val facePayService = mockk<FacePayService>()
        val paymentService = mockk<PaymentService>()
        val userId = UUID.randomUUID()

        beforeEach {
            restDocumentation.beforeTest(KioskFacePayControllerDocsTest::class.java, it.name.name)
            mockMvc =
                MockMvcBuilders
                    .standaloneSetup(KioskFacePayController(facePayService, paymentService))
                    .apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
                    .build()
        }

        afterEach {
            restDocumentation.afterTest()
        }

        test("얼굴 인식 - MATCHED") {
            every { facePayService.recognize(any()) } returns
                RecognitionResult(
                    FaceRecognitionStatus.MATCHED,
                    listOf(FaceCandidate(userId, "홍길동")),
                )

            mockMvc
                .post("/api/v1/kiosk/face-pay/recognize") {
                    contentType = MediaType.APPLICATION_JSON
                    content =
                        objectMapper.writeValueAsString(
                            mapOf("embedding" to List(128) { 0.1f }),
                        )
                }.andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "kiosk-face-pay-recognize",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                fieldWithPath("embedding[]").description("128차원 얼굴 임베딩"),
                            ),
                            responseFields(
                                fieldWithPath("status").description("인식 결과 (MATCHED, CANDIDATES, NO_MATCH)"),
                                fieldWithPath("candidates[].userId").description("후보 유저 ID"),
                                fieldWithPath("candidates[].name").description("후보 유저 이름"),
                            ),
                        ),
                    )
                }
        }

        test("페이스페이 결제 확인") {
            val orderId = UUID.randomUUID()
            every { paymentService.confirmPayment(code = "123456", userId = userId) } returns
                PaymentConfirmResult(orderId, 1, 6000, 4000)

            mockMvc
                .post("/api/v1/kiosk/face-pay/confirm") {
                    contentType = MediaType.APPLICATION_JSON
                    content =
                        objectMapper.writeValueAsString(
                            mapOf("code" to "123456", "userId" to userId),
                        )
                }.andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "kiosk-face-pay-confirm",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                fieldWithPath("code").description("6자리 결제 코드"),
                                fieldWithPath("userId").description("결제할 유저 ID"),
                            ),
                            responseFields(
                                fieldWithPath("orderId").description("주문 ID"),
                                fieldWithPath("orderNumber").description("주문 번호"),
                                fieldWithPath("totalAmount").description("총 결제 금액"),
                                fieldWithPath("balanceAfter").description("결제 후 잔액"),
                            ),
                        ),
                    )
                }
        }
    })
