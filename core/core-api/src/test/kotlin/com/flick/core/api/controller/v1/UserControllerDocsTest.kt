package com.flick.core.api.controller.v1

import com.flick.core.domain.user.UserService
import com.flick.core.enums.UserRole
import com.flick.storage.db.core.entity.User
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.util.UUID

@Tag("restdocs")
class UserControllerDocsTest :
    FunSpec({
        val restDocumentation = ManualRestDocumentation()
        lateinit var mockMvc: MockMvc
        val userService = mockk<UserService>()
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
            restDocumentation.beforeTest(UserControllerDocsTest::class.java, it.name.name)
            mockMvc =
                MockMvcBuilders
                    .standaloneSetup(UserController(userService))
                    .setCustomArgumentResolvers(authResolver)
                    .apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
                    .build()
        }

        afterEach {
            restDocumentation.afterTest()
        }

        test("내 정보 조회") {
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
            every { userService.getUser(userId) } returns user

            mockMvc
                .get("/api/v1/users/me")
                .andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "user-me",
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                fieldWithPath("id").description("유저 ID"),
                                fieldWithPath("name").description("이름"),
                                fieldWithPath("email").description("이메일"),
                                fieldWithPath("role").description("역할 (STUDENT, TEACHER, ADMIN)"),
                                fieldWithPath("balance").description("잔액"),
                                fieldWithPath("grade").description("학년").optional(),
                                fieldWithPath("room").description("반").optional(),
                                fieldWithPath("number").description("번호").optional(),
                                fieldWithPath("isGrantClaimed").description("지원금 수령 여부"),
                            ),
                        ),
                    )
                }
        }
    })
