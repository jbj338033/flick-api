package com.flick.core.config

import com.flick.storage.redis.KioskSessionRepository
import com.flick.support.security.KioskSessionFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KioskSecurityConfig(
    private val kioskSessionRepository: KioskSessionRepository,
) {
    @Bean
    fun kioskSessionFilter(): KioskSessionFilter = KioskSessionFilter(kioskSessionRepository::findBoothIdByToken)
}
