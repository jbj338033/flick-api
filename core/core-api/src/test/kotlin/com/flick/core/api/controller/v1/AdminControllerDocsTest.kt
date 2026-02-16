package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.ChargeRequest
import com.flick.core.domain.admin.AdminService
import com.flick.core.domain.admin.BoothStat
import com.flick.core.domain.admin.DashboardStats
import com.flick.core.domain.admin.SettlementExportService
import com.flick.core.domain.booth.BoothService
import com.flick.core.domain.charge.ChargeService
import com.flick.core.domain.order.OrderService
import com.flick.core.domain.product.ProductService
import com.flick.core.domain.user.UserService
import com.flick.core.enums.UserRole
import com.flick.storage.db.core.entity.User
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.util.UUID

@Tag("restdocs")
class AdminControllerDocsTest :
    FunSpec({
        val restDocumentation = ManualRestDocumentation()
        lateinit var mockMvc: MockMvc
        val objectMapper = jsonMapper { addModule(kotlinModule()) }
        val adminService = mockk<AdminService>()
        val userService = mockk<UserService>()
        val boothService = mockk<BoothService>()
        val orderService = mockk<OrderService>()
        val productService = mockk<ProductService>()
        val settlementExportService = mockk<SettlementExportService>()
        val chargeService = mockk<ChargeService>()

        beforeEach {
            restDocumentation.beforeTest(AdminControllerDocsTest::class.java, it.name.name)
            mockMvc =
                MockMvcBuilders
                    .standaloneSetup(
                        AdminController(
                            adminService,
                            userService,
                            boothService,
                            orderService,
                            productService,
                            settlementExportService,
                            chargeService,
                        ),
                    ).apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
                    .build()
        }

        afterEach {
            restDocumentation.afterTest()
        }

        test("대시보드 통계 조회") {
            every { adminService.getDashboardStats() } returns
                DashboardStats(
                    totalUsers = 240,
                    totalBooths = 12,
                    totalOrders = 350,
                    totalBalance = 1200000,
                    totalCharged = 2400000,
                    totalSpent = 1200000,
                    boothStats =
                        listOf(
                            BoothStat(UUID.randomUUID(), "떡볶이 부스", 500000, 100),
                        ),
                )

            mockMvc
                .get("/api/v1/admin/stats")
                .andExpect { status { isOk() } }
                .andDo {
                    handle(
                        document(
                            "admin-stats",
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                fieldWithPath("totalUsers").description("전체 유저 수"),
                                fieldWithPath("totalBooths").description("전체 부스 수"),
                                fieldWithPath("totalOrders").description("전체 주문 수"),
                                fieldWithPath("totalBalance").description("전체 잔액 합계"),
                                fieldWithPath("totalCharged").description("전체 충전 합계"),
                                fieldWithPath("totalSpent").description("전체 소비 합계"),
                                fieldWithPath("boothStats[].boothId").description("부스 ID"),
                                fieldWithPath("boothStats[].name").description("부스 이름"),
                                fieldWithPath("boothStats[].totalSales").description("부스 총 매출"),
                                fieldWithPath("boothStats[].orderCount").description("부스 주문 수"),
                            ),
                        ),
                    )
                }
        }

        test("전체 유저 조회") {
            val user =
                User(
                    dauthId = "dauth123",
                    name = "홍길동",
                    email = "hong@dsm.hs.kr",
                    role = UserRole.STUDENT,
                    grade = 1,
                    room = 1,
                    number = 1,
                )
            every { userService.getAllUsers() } returns listOf(user)

            mockMvc
                .get("/api/v1/admin/users")
                .andExpect { status { isOk() } }
                .andDo {
                    handle(
                        document(
                            "admin-users",
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                fieldWithPath("[].id").description("유저 ID"),
                                fieldWithPath("[].name").description("이름"),
                                fieldWithPath("[].email").description("이메일"),
                                fieldWithPath("[].role").description("역할"),
                                fieldWithPath("[].balance").description("잔액"),
                                fieldWithPath("[].grade").description("학년").optional(),
                                fieldWithPath("[].room").description("반").optional(),
                                fieldWithPath("[].number").description("번호").optional(),
                                fieldWithPath("[].isGrantClaimed").description("지원금 수령 여부"),
                            ),
                        ),
                    )
                }
        }

        test("충전") {
            every { chargeService.charge(any(), any(), any(), any()) } just runs

            mockMvc
                .post("/api/v1/admin/charge") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(ChargeRequest(1, 1, 1, 5000))
                }.andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "charge",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                fieldWithPath("grade").description("학년"),
                                fieldWithPath("room").description("반"),
                                fieldWithPath("number").description("번호"),
                                fieldWithPath("amount").description("충전 금액"),
                            ),
                        ),
                    )
                }
        }
    })
