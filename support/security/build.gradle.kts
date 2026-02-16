dependencies {
    api("org.springframework.boot:spring-boot-starter-security")
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    api("io.jsonwebtoken:jjwt-api:${property("jjwtVersion")}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${property("jjwtVersion")}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${property("jjwtVersion")}")
}
