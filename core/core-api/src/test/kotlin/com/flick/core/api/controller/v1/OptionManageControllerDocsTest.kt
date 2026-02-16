package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.UpdateOptionRequest
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.util.UUID

@Tag("restdocs")
class OptionManageControllerDocsTest :
    FunSpec({
        val restDocumentation = ManualRestDocumentation()
        lateinit var mockMvc: MockMvc
        val objectMapper = jsonMapper { addModule(kotlinModule()) }
        val productOptionService = mockk<ProductOptionService>()
        val userId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

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
            restDocumentation.beforeTest(OptionManageControllerDocsTest::class.java, it.name.name)
            mockMvc =
                MockMvcBuilders
                    .standaloneSetup(OptionManageController(productOptionService))
                    .setCustomArgumentResolvers(authResolver)
                    .apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
                    .build()
        }

        afterEach {
            restDocumentation.afterTest()
        }

        test("옵션 수정") {
            val updated = ProductOptionResponse(id = optionId, name = "엑스라지", price = 1000, isQuantitySelectable = false, sortOrder = 0)
            every { productOptionService.updateOption(userId, optionId, "엑스라지", 1000, null, null) } returns updated

            mockMvc
                .patch("/api/v1/options/$optionId") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(UpdateOptionRequest(name = "엑스라지", price = 1000))
                }.andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "option-update",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                fieldWithPath("name").description("옵션 이름").optional(),
                                fieldWithPath("price").description("옵션 가격").optional(),
                                fieldWithPath("isQuantitySelectable").description("수량 선택 가능 여부").optional(),
                                fieldWithPath("sortOrder").description("정렬 순서").optional(),
                            ),
                            responseFields(
                                fieldWithPath("id").description("옵션 ID"),
                                fieldWithPath("name").description("옵션 이름"),
                                fieldWithPath("price").description("옵션 가격"),
                                fieldWithPath("isQuantitySelectable").description("수량 선택 가능 여부"),
                                fieldWithPath("sortOrder").description("정렬 순서"),
                            ),
                        ),
                    )
                }
        }

        test("옵션 삭제") {
            every { productOptionService.deleteOption(userId, optionId) } returns Unit

            mockMvc
                .delete("/api/v1/options/$optionId")
                .andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "option-delete",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                        ),
                    )
                }
        }
    })
