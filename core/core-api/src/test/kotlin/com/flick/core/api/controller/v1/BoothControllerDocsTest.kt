package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.CreateBoothRequest
import com.flick.core.api.controller.v1.request.UpdateBoothRequest
import com.flick.core.domain.booth.BoothService
import com.flick.core.domain.order.OrderService
import com.flick.storage.db.core.entity.Booth
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
import org.springframework.test.web.servlet.put
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
class BoothControllerDocsTest :
    FunSpec({
        val restDocumentation = ManualRestDocumentation()
        lateinit var mockMvc: MockMvc
        val objectMapper = jsonMapper { addModule(kotlinModule()) }
        val boothService = mockk<BoothService>()
        val orderService = mockk<OrderService>()
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
            restDocumentation.beforeTest(BoothControllerDocsTest::class.java, it.name.name)
            mockMvc =
                MockMvcBuilders
                    .standaloneSetup(BoothController(boothService, orderService))
                    .setCustomArgumentResolvers(authResolver)
                    .apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
                    .build()
        }

        afterEach {
            restDocumentation.afterTest()
        }

        val boothFields =
            responseFields(
                fieldWithPath("id").description("부스 ID"),
                fieldWithPath("name").description("부스 이름"),
                fieldWithPath("description").description("부스 설명").optional(),
                fieldWithPath("createdAt").description("생성 시각"),
            )

        val boothOwnerFields =
            responseFields(
                fieldWithPath("id").description("부스 ID"),
                fieldWithPath("name").description("부스 이름"),
                fieldWithPath("description").description("부스 설명").optional(),
                fieldWithPath("pairingCode").description("키오스크 페어링 코드").optional(),
                fieldWithPath("createdAt").description("생성 시각"),
            )

        val boothListFields =
            responseFields(
                fieldWithPath("[].id").description("부스 ID"),
                fieldWithPath("[].name").description("부스 이름"),
                fieldWithPath("[].description").description("부스 설명").optional(),
                fieldWithPath("[].createdAt").description("생성 시각"),
            )

        val user = User(dauthId = "test", name = "테스터", email = "t@t.com")

        fun createBooth(name: String = "떡볶이 부스") = Booth(name = name, description = "맛있는 떡볶이", pairingCode = "1234", user = user)

        test("부스 목록 조회") {
            every { boothService.getAllBooths() } returns listOf(createBooth())

            mockMvc
                .get("/api/v1/booths")
                .andExpect { status { isOk() } }
                .andDo { handle(document("booth-list", preprocessResponse(prettyPrint()), boothListFields)) }
        }

        test("부스 상세 조회") {
            val booth = createBooth()
            every { boothService.getBooth(booth.id) } returns booth

            mockMvc
                .get("/api/v1/booths/${booth.id}")
                .andExpect { status { isOk() } }
                .andDo { handle(document("booth-detail", preprocessResponse(prettyPrint()), boothFields)) }
        }

        test("내 부스 조회") {
            every { boothService.getMyBooth(userId) } returns createBooth()

            mockMvc
                .get("/api/v1/booths/me")
                .andExpect { status { isOk() } }
                .andDo { handle(document("booth-me", preprocessResponse(prettyPrint()), boothOwnerFields)) }
        }

        test("부스 생성") {
            every { boothService.createBooth(any(), any(), any()) } returns createBooth()

            mockMvc
                .post("/api/v1/booths") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(CreateBoothRequest("떡볶이 부스", "맛있는 떡볶이"))
                }.andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "booth-create",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                fieldWithPath("name").description("부스 이름"),
                                fieldWithPath("description").description("부스 설명").optional(),
                            ),
                            boothOwnerFields,
                        ),
                    )
                }
        }

        test("부스 수정") {
            val booth = createBooth()
            every { boothService.verifyOwner(booth.id, userId) } returns Unit
            every { boothService.updateBooth(any(), any(), any()) } returns
                booth.apply {
                    name = "새 이름"
                    description = "새 설명"
                }

            mockMvc
                .put("/api/v1/booths/${booth.id}") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(UpdateBoothRequest("새 이름", "새 설명"))
                }.andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "booth-update",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                fieldWithPath("name").description("변경할 부스 이름").optional(),
                                fieldWithPath("description").description("변경할 부스 설명").optional(),
                            ),
                            boothOwnerFields,
                        ),
                    )
                }
        }

        test("페어링 코드 재발급") {
            val booth = createBooth()
            every { boothService.verifyOwner(booth.id, userId) } returns Unit
            every { boothService.generatePairingCode(booth.id) } returns booth.apply { pairingCode = "5678" }

            mockMvc
                .post("/api/v1/booths/${booth.id}/pairing-code")
                .andExpect { status { isOk() } }
                .andDo {
                    handle(
                        document(
                            "booth-pairing-code",
                            preprocessResponse(prettyPrint()),
                            boothOwnerFields,
                        ),
                    )
                }
        }
    })
