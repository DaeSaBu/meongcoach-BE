# jar는 CI(ci.yml)가 test → bootJar로 만들고 CD가 artifact로 내려받는다. 로컬은 `./gradlew bootJar` 뒤 `docker compose --profile app up --build`.
# 이미지 안에서 bootJar를 돌리면 스펙 생성 체인(test → openapi3)의 test가 Testcontainers로 Docker 데몬을 요구해 실패한다
FROM eclipse-temurin:25-jre

RUN useradd --system --uid 10001 app
WORKDIR /app

COPY --chown=app:app build/libs/*.jar app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
