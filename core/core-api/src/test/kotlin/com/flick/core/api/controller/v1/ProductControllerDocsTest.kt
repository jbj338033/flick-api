package com.flick.core.api.controller.v1

import com.flick.core.api.controller.v1.request.CreateProductRequest
import com.flick.core.api.controller.v1.request.UpdateProductRequest
import com.flick.core.domain.booth.BoothService
import com.flick.core.domain.product.ProductOptionService
import com.flick.core.domain.product.ProductService
import com.flick.storage.db.core.entity.Booth
import com.flick.storage.db.core.entity.Product
import com.flick.storage.db.core.entity.User
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
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
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
import java.util.UUID

@Tag("restdocs")
class ProductControllerDocsTest :
    FunSpec({
        val restDocumentation = ManualRestDocumentation()
        lateinit var mockMvc: MockMvc
        val objectMapper = jsonMapper { addModule(kotlinModule()) }
        val productService = mockk<ProductService>()
        val productOptionService = mockk<ProductOptionService>()
        val boothService = mockk<BoothService>()
        val user = User(dauthId = "test", name = "테스터", email = "t@t.com")
        val booth = Booth(name = "테스트 부스", user = user)
        val boothId = booth.id
        val userId = user.id

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
            restDocumentation.beforeTest(ProductControllerDocsTest::class.java, it.name.name)
            mockMvc =
                MockMvcBuilders
                    .standaloneSetup(
                        ProductController(productService, productOptionService, boothService),
                        ProductManageController(productService, productOptionService),
                    ).setCustomArgumentResolvers(authResolver)
                    .apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
                    .build()
        }

        afterEach {
            restDocumentation.afterTest()
        }

        val productFields =
            responseFields(
                fieldWithPath("id").description("상품 ID"),
                fieldWithPath("name").description("상품명"),
                fieldWithPath("price").description("가격"),
                fieldWithPath("stock").description("재고 (null=무제한)").optional(),
                fieldWithPath("isSoldOut").description("품절 여부"),
                fieldWithPath("purchaseLimit").description("1인당 구매 제한 (null=무제한)").optional(),
                fieldWithPath("booth.id").description("부스 ID"),
                fieldWithPath("booth.name").description("부스 이름"),
                fieldWithPath("optionGroups").description("옵션 그룹 목록"),
            )

        val productListFields =
            responseFields(
                fieldWithPath("[].id").description("상품 ID"),
                fieldWithPath("[].name").description("상품명"),
                fieldWithPath("[].price").description("가격"),
                fieldWithPath("[].stock").description("재고 (null=무제한)").optional(),
                fieldWithPath("[].isSoldOut").description("품절 여부"),
                fieldWithPath("[].purchaseLimit").description("1인당 구매 제한 (null=무제한)").optional(),
                fieldWithPath("[].booth.id").description("부스 ID"),
                fieldWithPath("[].booth.name").description("부스 이름"),
                fieldWithPath("[].optionGroups").description("옵션 그룹 목록"),
            )

        fun createProduct(
            name: String = "떡볶이",
            price: Int = 3000,
        ) = Product(name = name, price = price, stock = 50, booth = booth)

        test("상품 목록 조회") {
            every { productService.getProductsByBooth(boothId) } returns listOf(createProduct(), createProduct("순대", 4000))
            every { productOptionService.getOptionGroupResponsesByProductIds(any()) } returns emptyMap()

            mockMvc
                .get("/api/v1/booths/$boothId/products")
                .andExpect { status { isOk() } }
                .andDo { handle(document("product-list", preprocessResponse(prettyPrint()), productListFields)) }
        }

        test("상품 생성") {
            every { boothService.verifyOwner(boothId, userId) } just runs
            every { productService.createProduct(any(), any(), any(), any(), any()) } returns createProduct()
            every { productOptionService.getOptionGroupsByProduct(any()) } returns emptyList()

            mockMvc
                .post("/api/v1/booths/$boothId/products") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(CreateProductRequest("떡볶이", 3000, 50))
                }.andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "product-create",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                fieldWithPath("name").description("상품명"),
                                fieldWithPath("price").description("가격"),
                                fieldWithPath("stock").description("재고 (null=무제한)").optional(),
                                fieldWithPath("optionGroups").description("옵션 그룹 목록").optional(),
                            ),
                            productFields,
                        ),
                    )
                }
        }

        test("상품 수정 (PATCH)") {
            val product = createProduct()
            every { productService.updateProduct(any(), any(), any(), any(), any(), any(), any()) } returns
                product.apply {
                    name = "떡볶이 대"
                    price = 4000
                }
            every { productOptionService.getOptionGroupsByProduct(any()) } returns emptyList()

            mockMvc
                .patch("/api/v1/products/${product.id}") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(UpdateProductRequest("떡볶이 대", 4000))
                }.andExpect {
                    status { isOk() }
                }.andDo {
                    handle(
                        document(
                            "product-patch",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                fieldWithPath("name").description("변경할 상품명").optional(),
                                fieldWithPath("price").description("변경할 가격").optional(),
                                fieldWithPath("stock").description("변경할 재고").optional(),
                                fieldWithPath("purchaseLimit").description("변경할 구매 제한").optional(),
                                fieldWithPath("isSoldOut").description("품절 여부").optional(),
                            ),
                            productFields,
                        ),
                    )
                }
        }

        test("상품 삭제") {
            val product = createProduct()
            every { productService.deleteProduct(any(), any()) } just runs

            mockMvc
                .delete("/api/v1/products/${product.id}")
                .andExpect { status { isOk() } }
                .andDo { handle(document("product-delete")) }
        }
    })
