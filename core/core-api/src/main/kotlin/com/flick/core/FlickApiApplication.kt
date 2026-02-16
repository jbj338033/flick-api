package com.flick.core

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.flick"])
@ConfigurationPropertiesScan(basePackages = ["com.flick"])
@EnableScheduling
class FlickApiApplication

fun main(args: Array<String>) {
    runApplication<FlickApiApplication>(*args)
}
