FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY core/core-api/build.gradle.kts core/core-api/
COPY core/core-enum/build.gradle.kts core/core-enum/
COPY storage/db-core/build.gradle.kts storage/db-core/
COPY storage/db-redis/build.gradle.kts storage/db-redis/
COPY support/logging/build.gradle.kts support/logging/
COPY support/security/build.gradle.kts support/security/
COPY support/monitoring/build.gradle.kts support/monitoring/
COPY clients/client-dauth/build.gradle.kts clients/client-dauth/
COPY tests/api-docs/build.gradle.kts tests/api-docs/
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true
COPY . .
RUN ./gradlew :core:core-api:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/core/core-api/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
