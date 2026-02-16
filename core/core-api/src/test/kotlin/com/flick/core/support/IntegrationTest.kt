package com.flick.core.support

import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Tag("context")
@SpringBootTest
@Import(TestcontainersConfig::class)
@ActiveProfiles("test")
annotation class IntegrationTest
