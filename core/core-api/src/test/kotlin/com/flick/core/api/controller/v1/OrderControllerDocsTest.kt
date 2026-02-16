package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.response.BoothRef
import com.flick.core.api.controller.v1.response.OrderItemResponse
import com.flick.core.api.controller.v1.response.OrderResponse
import com.flick.core.api.controller.v1.response.ProductRef
import com.flick.core.domain.order.OrderService
import com.flick.core.enums.OrderStatus
import com.flick.storage.db.core.entity.Booth
import com.flick.storage.db.core.entity.Order
import com.flick.storage.db.core.entity.Product
import com.flick.storage.db.core.entity.User
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.util.UUID

@Tag("restdocs")
class OrderControllerDocsTest :
    FunSpec({
        val restDocumentation = ManualRestDocumentation()
        lateinit var mockMvc: MockMvc
        val orderService = mockk<OrderService>()
        val userId = UUID.randomUUID()
        val user = User(dauthId = "test", name = "테스터", email = "t@t.com")
        val booth = Booth(name = "테스트 부스", user = user)

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
            restDocumentation.beforeTest(OrderControllerDocsTest::class.java, it.name.name)
            mockMvc =
                MockMvcBuilders
                    .standaloneSetup(OrderController(orderService))
                    .setCustomArgumentResolvers(authResolver)
                    .apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
                    .build()
        }

        afterEach {
            restDocumentation.afterTest()
        }

        val orderListFields =
            responseFields(
                fieldWithPath("[].id").description("주문 ID"),
                fieldWithPath("[].orderNumber").description("주문 번호"),
                fieldWithPath("[].totalAmount").description("총 금액"),
                fieldWithPath("[].status").description("주문 상태 (PENDING, CONFIRMED, CANCELLED)"),
                fieldWithPath("[].booth.id").description("부스 ID"),
                fieldWithPath("[].booth.name").description("부스 이름"),
                fieldWithPath("[].createdAt").description("주문 시각"),
                fieldWithPath("[].items[].product.id").description("상품 ID"),
                fieldWithPath("[].items[].product.name").description("상품명"),
                fieldWithPath("[].items[].quantity").description("수량"),
                fieldWithPath("[].items[].price").description("단가"),
                fieldWithPath("[].items[].options").description("선택 옵션 목록"),
            )

        fun createOrderResponse(): OrderResponse {
            val order = Order(orderNumber = 1, totalAmount = 6000, status = OrderStatus.CONFIRMED, booth = booth)
            return OrderResponse(
                id = order.id,
                orderNumber = 1,
                totalAmount = 6000,
                status = OrderStatus.CONFIRMED,
                booth = BoothRef(id = booth.id, name = booth.name),
                createdAt = order.createdAt,
                items =
                    listOf(
                        OrderItemResponse(
                            product = ProductRef(id = Product(name = "떡볶이", price = 3000, stock = 50, booth = booth).id, name = "떡볶이"),
                            quantity = 2,
                            price = 3000,
                        ),
                    ),
            )
        }

        test("내 주문 목록 조회") {
            val orderResponse = createOrderResponse()
            every { orderService.getOrdersWithItemsByUser(userId) } returns listOf(orderResponse)

            mockMvc
                .get("/api/v1/orders/me")
                .andExpect { status { isOk() } }
                .andDo { handle(document("order-my-list", preprocessResponse(prettyPrint()), orderListFields)) }
        }

        test("주문 취소") {
            val orderId = UUID.randomUUID()
            every { orderService.cancelOrder(any(), any()) } just runs

            mockMvc
                .patch("/api/v1/orders/$orderId/cancel")
                .andExpect { status { isOk() } }
                .andDo { handle(document("order-cancel")) }
        }
    })
