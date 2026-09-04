FROM eclipse-temurin:25-jdk AS builder

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src
# bootJar가 OpenAPI 스펙 생성 체인(test → openapi3)을 강제하므로 이미지에 불필요한 finalizer만 스킵한다
RUN ./gradlew bootJar -x jacocoTestReport -x asciidoctor --no-daemon

FROM eclipse-temurin:25-jre

RUN useradd --system --uid 10001 app
WORKDIR /app

COPY --from=builder --chown=app:app /workspace/build/libs/*.jar app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
