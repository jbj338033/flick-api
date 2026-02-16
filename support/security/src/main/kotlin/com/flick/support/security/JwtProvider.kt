package com.flick.support.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtProvider(
    private val properties: JwtProperties,
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(properties.secret.toByteArray())
    }

    fun createAccessToken(
        userId: UUID,
        role: String,
        tokenType: String,
    ): String = createToken(userId, role, tokenType, properties.accessExpiration)

    fun createRefreshToken(
        userId: UUID,
        tokenType: String,
    ): String = createToken(userId, null, tokenType, properties.refreshExpiration)

    private fun createToken(
        userId: UUID,
        role: String?,
        tokenType: String,
        expiration: Long,
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        val builder =
            Jwts
                .builder()
                .subject(userId.toString())
                .claim("tokenType", tokenType)
                .issuedAt(now)
                .expiration(expiryDate)

        if (role != null) {
            builder.claim("role", role)
        }

        return builder.signWith(key).compact()
    }

    fun validateToken(token: String): TokenValidation =
        try {
            parseClaimsInternal(token)
            TokenValidation.Valid
        } catch (e: ExpiredJwtException) {
            TokenValidation.Expired
        } catch (e: JwtException) {
            TokenValidation.Invalid
        }

    fun getUserId(token: String): UUID = UUID.fromString(parseClaims(token).subject)

    fun getRole(token: String): String? = parseClaims(token)["role"] as? String

    fun parseClaims(token: String): Claims = parseClaimsInternal(token)

    private fun parseClaimsInternal(token: String): Claims =
        Jwts
            .parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}
