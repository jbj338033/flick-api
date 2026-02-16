package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.KioskPairRequest
import com.flick.core.domain.kiosk.KioskPairResult
import com.flick.core.domain.kiosk.KioskPairingService
import com.flick.core.domain.kiosk.KioskSseService
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
class KioskControllerDocsTest :
    FunSpec({
        val restDocumentation = ManualRestDocumentation()
        lateinit var mockMvc: MockMvc
        val objectMapper = jsonMapper { addModule(kotlinModule()) }
        val kioskPairingService = mockk<KioskPairingService>()
        val kioskSseService = mockk<KioskSseService>()

        beforeEach {
            restDocumentation.beforeTest(KioskControllerDocsTest::class.java, it.name.name)
            mockMvc =
                MockMvcBuilders
                    .standaloneSetup(KioskController(kioskSseService, kioskPairingService))
                    .apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
                    .build()
        }

        afterEach {
            restDocumentation.afterTest()
        }

        test("키오스크 페어링") {
            val boothId = UUID.randomUUID()
            every { kioskPairingService.pair("1234") } returns
                KioskPairResult(token = "session-token-uuid", boothId = boothId, boothName = "타코야끼")

            mockMvc
                .post("/api/v1/kiosk/pair") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(KioskPairRequest("1234"))
                }.andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "kiosk-pair",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                fieldWithPath("pairingCode").description("4자리 페어링 코드"),
                            ),
                            responseFields(
                                fieldWithPath("token").description("세션 토큰"),
                                fieldWithPath("booth.id").description("부스 ID"),
                                fieldWithPath("booth.name").description("부스 이름"),
                            ),
                        ),
                    )
                }
        }
    })
