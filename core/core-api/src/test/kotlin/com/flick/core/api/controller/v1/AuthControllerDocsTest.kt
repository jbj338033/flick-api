package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.LoginRequest
import com.flick.core.api.controller.v1.request.RefreshRequest
import com.flick.core.domain.auth.AuthService
import com.flick.core.domain.auth.TokenPair
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

@Tag("restdocs")
class AuthControllerDocsTest :
    FunSpec({
        val restDocumentation = ManualRestDocumentation()
        lateinit var mockMvc: MockMvc
        val objectMapper = jsonMapper { addModule(kotlinModule()) }
        val authService = mockk<AuthService>()

        beforeEach {
            restDocumentation.beforeTest(AuthControllerDocsTest::class.java, it.name.name)
            mockMvc =
                MockMvcBuilders
                    .standaloneSetup(AuthController(authService))
                    .apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
                    .build()
        }

        afterEach {
            restDocumentation.afterTest()
        }

        test("로그인") {
            every { authService.login("testid", "testpw") } returns TokenPair("access-token", "refresh-token")

            mockMvc
                .post("/api/v1/auth/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(LoginRequest("testid", "testpw"))
                }.andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "auth-login",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                fieldWithPath("id").description("DAuth 아이디"),
                                fieldWithPath("password").description("DAuth 비밀번호"),
                            ),
                            responseFields(
                                fieldWithPath("accessToken").description("액세스 토큰"),
                                fieldWithPath("refreshToken").description("리프레시 토큰"),
                            ),
                        ),
                    )
                }
        }

        test("토큰 갱신") {
            every { authService.refresh("old-refresh-token") } returns TokenPair("new-access", "new-refresh")

            mockMvc
                .post("/api/v1/auth/refresh") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(RefreshRequest("old-refresh-token"))
                }.andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "auth-refresh",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                fieldWithPath("refreshToken").description("리프레시 토큰"),
                            ),
                            responseFields(
                                fieldWithPath("accessToken").description("새 액세스 토큰"),
                                fieldWithPath("refreshToken").description("새 리프레시 토큰"),
                            ),
                        ),
                    )
                }
        }
    })
