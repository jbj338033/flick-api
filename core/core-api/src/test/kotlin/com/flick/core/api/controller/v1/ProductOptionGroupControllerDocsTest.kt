package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.CreateOptionGroupRequest
import com.flick.core.api.controller.v1.request.CreateOptionRequest
import com.flick.core.api.controller.v1.response.ProductOptionGroupResponse
import com.flick.core.api.controller.v1.response.ProductOptionResponse
import com.flick.core.domain.product.ProductOptionService
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
class ProductOptionGroupControllerDocsTest :
    FunSpec({
        val restDocumentation = ManualRestDocumentation()
        lateinit var mockMvc: MockMvc
        val objectMapper = jsonMapper { addModule(kotlinModule()) }
        val productOptionService = mockk<ProductOptionService>()
        val userId = UUID.randomUUID()
        val productId = UUID.randomUUID()

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
            restDocumentation.beforeTest(ProductOptionGroupControllerDocsTest::class.java, it.name.name)
            mockMvc =
                MockMvcBuilders
                    .standaloneSetup(ProductOptionGroupController(productOptionService))
                    .setCustomArgumentResolvers(authResolver)
                    .apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
                    .build()
        }

        afterEach {
            restDocumentation.afterTest()
        }

        val optionResponse =
            ProductOptionResponse(id = UUID.randomUUID(), name = "라지", price = 500, isQuantitySelectable = false, sortOrder = 0)
        val groupResponse =
            ProductOptionGroupResponse(
                id = UUID.randomUUID(),
                name = "사이즈",
                isRequired = true,
                maxSelections = 1,
                sortOrder = 0,
                options = listOf(optionResponse),
            )

        test("옵션 그룹 목록 조회") {
            every { productOptionService.getOptionGroupsByProduct(productId) } returns listOf(groupResponse)

            mockMvc
                .get("/api/v1/products/$productId/option-groups")
                .andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "option-group-list",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                fieldWithPath("[].id").description("옵션 그룹 ID"),
                                fieldWithPath("[].name").description("옵션 그룹 이름"),
                                fieldWithPath("[].isRequired").description("필수 여부"),
                                fieldWithPath("[].maxSelections").description("최대 선택 수"),
                                fieldWithPath("[].sortOrder").description("정렬 순서"),
                                fieldWithPath("[].options[].id").description("옵션 ID"),
                                fieldWithPath("[].options[].name").description("옵션 이름"),
                                fieldWithPath("[].options[].price").description("옵션 가격"),
                                fieldWithPath("[].options[].isQuantitySelectable").description("수량 선택 가능 여부"),
                                fieldWithPath("[].options[].sortOrder").description("옵션 정렬 순서"),
                            ),
                        ),
                    )
                }
        }

        test("옵션 그룹 생성") {
            every { productOptionService.createOptionGroup(userId, productId, any()) } returns groupResponse

            mockMvc
                .post("/api/v1/products/$productId/option-groups") {
                    contentType = MediaType.APPLICATION_JSON
                    content =
                        objectMapper.writeValueAsString(
                            CreateOptionGroupRequest(
                                name = "사이즈",
                                isRequired = true,
                                maxSelections = 1,
                                sortOrder = 0,
                                options = listOf(CreateOptionRequest("라지", 500, false, 0)),
                            ),
                        )
                }.andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "option-group-create",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                fieldWithPath("name").description("옵션 그룹 이름"),
                                fieldWithPath("isRequired").description("필수 여부"),
                                fieldWithPath("maxSelections").description("최대 선택 수"),
                                fieldWithPath("sortOrder").description("정렬 순서"),
                                fieldWithPath("options[].name").description("옵션 이름"),
                                fieldWithPath("options[].price").description("옵션 가격"),
                                fieldWithPath("options[].isQuantitySelectable").description("수량 선택 가능 여부"),
                                fieldWithPath("options[].sortOrder").description("옵션 정렬 순서"),
                            ),
                            responseFields(
                                fieldWithPath("id").description("옵션 그룹 ID"),
                                fieldWithPath("name").description("옵션 그룹 이름"),
                                fieldWithPath("isRequired").description("필수 여부"),
                                fieldWithPath("maxSelections").description("최대 선택 수"),
                                fieldWithPath("sortOrder").description("정렬 순서"),
                                fieldWithPath("options[].id").description("옵션 ID"),
                                fieldWithPath("options[].name").description("옵션 이름"),
                                fieldWithPath("options[].price").description("옵션 가격"),
                                fieldWithPath("options[].isQuantitySelectable").description("수량 선택 가능 여부"),
                                fieldWithPath("options[].sortOrder").description("옵션 정렬 순서"),
                            ),
                        ),
                    )
                }
        }
    })
