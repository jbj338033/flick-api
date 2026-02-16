package com.flick.core.domain.facepay

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.math.abs

class EmbeddingUtilsTest :
    FunSpec({
        context("toByteArray / toFloatArray 라운드트립") {
            test("변환 후 원본과 동일") {
                val original = floatArrayOf(1.0f, -2.5f, 3.14f, 0.0f)
                val bytes = EmbeddingUtils.toByteArray(original)
                val restored = EmbeddingUtils.toFloatArray(bytes)
                restored.size shouldBe original.size
                restored.forEachIndexed { i, v -> v shouldBe original[i] }
            }
        }

        context("toFloatArray 잘못된 바이트 크기") {
            test("4의 배수가 아니면 IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    EmbeddingUtils.toFloatArray(byteArrayOf(1, 2, 3))
                }
            }
        }

        context("cosineSimilarity") {
            test("동일 벡터 → ~1.0") {
                val v = floatArrayOf(1.0f, 2.0f, 3.0f)
                val similarity = EmbeddingUtils.cosineSimilarity(v, v)
                abs(similarity - 1.0) shouldBeLessThan 1e-6
            }

            test("직교 벡터 → ~0.0") {
                val a = floatArrayOf(1.0f, 0.0f)
                val b = floatArrayOf(0.0f, 1.0f)
                val similarity = EmbeddingUtils.cosineSimilarity(a, b)
                abs(similarity) shouldBeLessThan 1e-6
            }

            test("반대 벡터 → ~-1.0") {
                val a = floatArrayOf(1.0f, 2.0f)
                val b = floatArrayOf(-1.0f, -2.0f)
                val similarity = EmbeddingUtils.cosineSimilarity(a, b)
                abs(similarity + 1.0) shouldBeLessThan 1e-6
            }

            test("차원 불일치 → IllegalArgumentException") {
                val a = floatArrayOf(1.0f, 2.0f)
                val b = floatArrayOf(1.0f)
                shouldThrow<IllegalArgumentException> {
                    EmbeddingUtils.cosineSimilarity(a, b)
                }
            }

            test("영벡터 → 0.0") {
                val zero = floatArrayOf(0.0f, 0.0f)
                val v = floatArrayOf(1.0f, 2.0f)
                EmbeddingUtils.cosineSimilarity(zero, v) shouldBe 0.0
            }
        }
    })
