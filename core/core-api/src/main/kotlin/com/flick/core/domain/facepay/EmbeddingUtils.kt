package com.flick.core.domain.facepay

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

object EmbeddingUtils {
    fun toByteArray(floats: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(floats.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        floats.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    fun toFloatArray(bytes: ByteArray): FloatArray {
        require(bytes.size % Float.SIZE_BYTES == 0) { "Invalid byte array size: ${bytes.size}" }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(bytes.size / Float.SIZE_BYTES) { buffer.float }
    }

    fun cosineSimilarity(
        a: FloatArray,
        b: FloatArray,
    ): Double {
        require(a.size == b.size) { "Dimension mismatch: ${a.size} vs ${b.size}" }
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denominator = sqrt(normA) * sqrt(normB)
        if (denominator == 0.0) return 0.0
        return dot / denominator
    }
}
