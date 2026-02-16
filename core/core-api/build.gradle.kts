import org.springframework.boot.gradle.tasks.bundling.BootJar

apply(plugin = "org.asciidoctor.jvm.convert")

val asciidoctorExt: Configuration by configurations.creating

tasks.named<BootJar>("bootJar") {
    enabled = true
}

tasks.jar {
    enabled = false
}

dependencies {
    asciidoctorExt("org.springframework.restdocs:spring-restdocs-asciidoctor:3.0.4")

    implementation(project(":core:core-enum"))
    implementation(project(":storage:db-core"))
    implementation(project(":storage:db-redis"))
    implementation(project(":clients:client-dauth"))
    implementation(project(":support:logging"))
    implementation(project(":support:monitoring"))
    implementation(project(":support:security"))
    testImplementation(project(":tests:api-docs"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${property("springdocVersion")}")
    implementation("org.apache.poi:poi-ooxml:5.3.0")

    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql:${property("testcontainersVersion")}")
    testImplementation("com.redis:testcontainers-redis:2.2.4")
    testImplementation("org.springframework.security:spring-security-test")
}

val snippetsDir = file("build/generated-snippets")

tasks.register<Test>("restDocsTest") {
    description = "Run REST Docs tests"
    group = "verification"
    useJUnitPlatform()
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    include("**/*DocsTest*")
}

tasks.register<Test>("contextTest") {
    description = "Run integration tests with Spring context"
    group = "verification"
    useJUnitPlatform()
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    include("**/*IntegrationTest*")
}

tasks.named("asciidoctor") {
    dependsOn("restDocsTest")
    inputs.dir(snippetsDir)
}

tasks.named<org.asciidoctor.gradle.jvm.AsciidoctorTask>("asciidoctor") {
    configurations("asciidoctorExt")
    sources {
        include("index.adoc")
    }
    attributes(
        mapOf(
            "snippets" to snippetsDir.absolutePath,
            "source-highlighter" to "highlightjs",
            "toc" to "left",
            "toclevels" to "3",
            "sectlinks" to "",
        ),
    )
}
