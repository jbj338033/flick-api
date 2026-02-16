package com.flick.storage.redis

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration
import java.util.UUID

@Repository
class PaymentCodeRepository(
    private val redisTemplate: StringRedisTemplate,
) {
    fun save(
        code: String,
        orderId: UUID,
        ttl: Duration = Duration.ofMinutes(3),
    ) {
        redisTemplate.opsForValue().set(key(code), orderId.toString(), ttl)
    }

    fun saveIfAbsent(
        code: String,
        orderId: UUID,
        ttl: Duration = Duration.ofMinutes(3),
    ): Boolean = redisTemplate.opsForValue().setIfAbsent(key(code), orderId.toString(), ttl) ?: false

    fun findOrderIdByCode(code: String): UUID? = redisTemplate.opsForValue().get(key(code))?.let { UUID.fromString(it) }

    fun delete(code: String) = redisTemplate.delete(key(code))

    fun exists(code: String): Boolean = redisTemplate.hasKey(key(code))

    private fun key(code: String) = "payment:code:$code"
}
