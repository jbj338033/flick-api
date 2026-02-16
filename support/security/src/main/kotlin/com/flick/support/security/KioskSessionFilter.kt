package com.flick.support.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

class KioskSessionFilter(
    private val resolveBoothId: (String) -> UUID?,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = request.getHeader("X-Kiosk-Token")

        if (token != null && request.requestURI.startsWith("/api/v1/kiosk/") && request.requestURI != "/api/v1/kiosk/pair") {
            val boothId = resolveBoothId(token)
            if (boothId != null) {
                val principal = KioskPrincipal(boothId)
                val authorities = listOf(SimpleGrantedAuthority("ROLE_KIOSK"))
                val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)
                SecurityContextHolder.getContext().authentication = authentication
            }
        }

        filterChain.doFilter(request, response)
    }
}
