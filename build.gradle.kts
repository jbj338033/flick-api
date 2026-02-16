plugins {
    kotlin("jvm")
    kotlin("plugin.jpa") apply false
    kotlin("plugin.spring") apply false
    kotlin("plugin.allopen") apply false
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
    id("org.asciidoctor.jvm.convert") apply false
    id("org.jlleitschuh.gradle.ktlint") apply false
}

allprojects {
    group = "${property("group")}"
    version = "${property("version")}"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("tools.jackson.module:jackson-module-kotlin")
        annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

        testImplementation("io.kotest:kotest-runner-junit5:${property("kotestVersion")}")
        testImplementation("io.kotest:kotest-assertions-core:${property("kotestVersion")}")
        testImplementation("io.kotest:kotest-property:${property("kotestVersion")}")
        testImplementation("io.kotest:kotest-extensions-spring:${property("kotestVersion")}")
        testImplementation("io.mockk:mockk:${property("mockkVersion")}")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of("${property("javaVersion")}")
        }
    }

    kotlin {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
        }
    }

    tasks.named("bootJar") {
        enabled = false
    }

    tasks.jar {
        enabled = true
    }

    tasks.test {
        useJUnitPlatform()
    }
}
