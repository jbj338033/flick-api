package com.flick.client.dauth

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(DAuthProperties::class)
class DAuthConfig {
    @Bean
    fun dauthRestClient(): RestClient =
        RestClient
            .builder()
            .baseUrl("https://dauth.b1nd.com")
            .build()

    @Bean
    fun dodamRestClient(): RestClient =
        RestClient
            .builder()
            .baseUrl("https://opendodam.b1nd.com")
            .build()
}
