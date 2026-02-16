package com.flick.storage.redis

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration
import java.util.UUID

@Repository
class KioskSessionRepository(
    private val redisTemplate: StringRedisTemplate,
) {
    fun save(
        token: String,
        boothId: UUID,
        ttl: Duration = Duration.ofHours(24),
    ) {
        redisTemplate.opsForValue().set(sessionKey(token), boothId.toString(), ttl)
        redisTemplate.opsForSet().add(boothSessionsKey(boothId), token)
        redisTemplate.expire(boothSessionsKey(boothId), ttl)
    }

    fun findBoothIdByToken(token: String): UUID? = redisTemplate.opsForValue().get(sessionKey(token))?.let { UUID.fromString(it) }

    fun countByBoothId(boothId: UUID): Long = redisTemplate.opsForSet().size(boothSessionsKey(boothId)) ?: 0

    fun deleteAllByBoothId(boothId: UUID) {
        val tokens = redisTemplate.opsForSet().members(boothSessionsKey(boothId)) ?: return
        tokens.forEach { token -> redisTemplate.delete(sessionKey(token)) }
        redisTemplate.delete(boothSessionsKey(boothId))
    }

    private fun sessionKey(token: String) = "kiosk:session:$token"

    private fun boothSessionsKey(boothId: UUID) = "kiosk:booth:$boothId:sessions"
}
