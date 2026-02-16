package com.flick.core.api.controller.v1

import com.flick.core.domain.facepay.FacePayService
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
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
class FacePayControllerDocsTest :
    FunSpec({
        val restDocumentation = ManualRestDocumentation()
        lateinit var mockMvc: MockMvc
        val objectMapper = jsonMapper { addModule(kotlinModule()) }
        val facePayService = mockk<FacePayService>()
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
            restDocumentation.beforeTest(FacePayControllerDocsTest::class.java, it.name.name)
            mockMvc =
                MockMvcBuilders
                    .standaloneSetup(FacePayController(facePayService))
                    .setCustomArgumentResolvers(authResolver)
                    .apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
                    .build()
        }

        afterEach {
            restDocumentation.afterTest()
        }

        test("얼굴 임베딩 등록") {
            every { facePayService.register(userId, any()) } just runs

            val embedding = List(128) { 0.1f }

            mockMvc
                .post("/api/v1/face-pay") {
                    contentType = MediaType.APPLICATION_JSON
                    content =
                        objectMapper.writeValueAsString(
                            mapOf("embeddings" to listOf(embedding, embedding)),
                        )
                }.andExpect {
                    status { isCreated() }
                }.andDo {
                    handle(
                        document(
                            "face-pay-register",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                fieldWithPath("embeddings[][]").description("128차원 얼굴 임베딩 배열 (다중 각도)"),
                            ),
                        ),
                    )
                }
        }

        test("얼굴 임베딩 삭제") {
            every { facePayService.delete(userId) } just runs

            mockMvc
                .delete("/api/v1/face-pay")
                .andExpect { status { isNoContent() } }
                .andDo {
                    handle(
                        document(
                            "face-pay-delete",
                            preprocessResponse(prettyPrint()),
                        ),
                    )
                }
        }
    })
