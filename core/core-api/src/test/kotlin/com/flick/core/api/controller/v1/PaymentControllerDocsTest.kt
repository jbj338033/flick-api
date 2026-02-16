package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.CreatePaymentRequest
import com.flick.core.api.controller.v1.request.PaymentItem
import com.flick.core.domain.payment.PaymentConfirmResult
import com.flick.core.domain.payment.PaymentRequestDetailResult
import com.flick.core.domain.payment.PaymentRequestItemResult
import com.flick.core.domain.payment.PaymentRequestResult
import com.flick.core.domain.payment.PaymentService
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
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.time.LocalDateTime
import java.util.UUID

@Tag("restdocs")
class PaymentControllerDocsTest :
    FunSpec({
        val restDocumentation = ManualRestDocumentation()
        lateinit var mockMvc: MockMvc
        val objectMapper = jsonMapper { addModule(kotlinModule()) }
        val paymentService = mockk<PaymentService>()
        val userId = UUID.randomUUID()
        val boothId = UUID.randomUUID()
        val productId = UUID.randomUUID()

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
            restDocumentation.beforeTest(PaymentControllerDocsTest::class.java, it.name.name)
            mockMvc =
                MockMvcBuilders
                    .standaloneSetup(PaymentController(paymentService))
                    .setCustomArgumentResolvers(authResolver)
                    .apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
                    .build()
        }

        afterEach {
            restDocumentation.afterTest()
        }

        test("결제 요청 생성") {
            val orderId = UUID.randomUUID()
            every { paymentService.createPaymentRequest(any()) } returns PaymentRequestResult(orderId, "123456", 1, 6000)

            mockMvc
                .post("/api/v1/payment-requests") {
                    contentType = MediaType.APPLICATION_JSON
                    content =
                        objectMapper.writeValueAsString(
                            CreatePaymentRequest(boothId, listOf(PaymentItem(productId, 2))),
                        )
                }.andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "payment-request-create",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                fieldWithPath("boothId").description("부스 ID"),
                                fieldWithPath("items[].productId").description("상품 ID"),
                                fieldWithPath("items[].quantity").description("수량"),
                                fieldWithPath("items[].options").description("선택 옵션 목록").optional(),
                            ),
                            responseFields(
                                fieldWithPath("orderId").description("주문 ID"),
                                fieldWithPath("code").description("6자리 결제 코드"),
                                fieldWithPath("orderNumber").description("주문 번호"),
                                fieldWithPath("totalAmount").description("총 결제 금액"),
                            ),
                        ),
                    )
                }
        }

        test("결제 요청 조회") {
            val orderId = UUID.randomUUID()
            every { paymentService.getPaymentRequestByCode("123456") } returns
                PaymentRequestDetailResult(
                    orderId = orderId,
                    code = "123456",
                    orderNumber = 1,
                    totalAmount = 6000,
                    boothId = boothId,
                    boothName = "테스트 부스",
                    confirmed = false,
                    expired = false,
                    expiresAt = LocalDateTime.now().plusMinutes(3),
                    items =
                        listOf(
                            PaymentRequestItemResult(productId, "떡볶이", 2, 3000),
                        ),
                )

            mockMvc
                .get("/api/v1/payment-requests/123456")
                .andExpect { status { isOk() } }
                .andDo {
                    handle(
                        document(
                            "payment-request-get",
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                fieldWithPath("orderId").description("주문 ID"),
                                fieldWithPath("code").description("6자리 결제 코드"),
                                fieldWithPath("orderNumber").description("주문 번호"),
                                fieldWithPath("totalAmount").description("총 결제 금액"),
                                fieldWithPath("booth.id").description("부스 ID"),
                                fieldWithPath("booth.name").description("부스 이름"),
                                fieldWithPath("confirmed").description("결제 확인 여부"),
                                fieldWithPath("expired").description("만료 여부"),
                                fieldWithPath("expiresAt").description("만료 시각"),
                                fieldWithPath("items[].product.id").description("상품 ID"),
                                fieldWithPath("items[].product.name").description("상품명"),
                                fieldWithPath("items[].quantity").description("수량"),
                                fieldWithPath("items[].price").description("단가"),
                            ),
                        ),
                    )
                }
        }

        test("결제 확인") {
            val orderId = UUID.randomUUID()
            every { paymentService.confirmPayment(any(), any()) } returns
                PaymentConfirmResult(orderId, 1, 6000, 4000)

            mockMvc
                .patch("/api/v1/payment-requests/123456")
                .andExpect { status { isOk() } }
                .andDo {
                    handle(
                        document(
                            "payment-request-confirm",
                            preprocessResponse(prettyPrint()),
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
