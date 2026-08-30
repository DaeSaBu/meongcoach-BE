# Back-End
DaeSaBu's Back-End Repository

## 로컬 실행

프로파일을 지정하지 않으면 `local`로 기동하며 PostgreSQL 18.3을 사용합니다. `spring-boot-docker-compose`가
`compose.yml`의 postgres를 자동으로 기동·연결하므로 Docker만 실행 중이면 별도의 DB 준비 없이
`./gradlew bootRun`(또는 IDE Run)만으로 실행됩니다. DB 데이터는 컨테이너가 시작될 때마다 초기화되며,
애플리케이션을 종료하면 postgres 컨테이너도 함께 정지됩니다. dev/prod 프로파일은
[docs/profiles.md](docs/profiles.md)를 참고하세요.

다음 환경 변수를 설정해야 애플리케이션이 기동합니다. 각 변수의 의미와 제약은
[docs/security.md](docs/security.md#환경-변수)를 참고하세요.

```bash
export JWT_SECRET=local-dev-secret-key-at-least-32-bytes-long
export KAKAO_NATIVE_APP_KEY=<카카오 콘솔의 네이티브 앱 키>
export KAKAO_REST_API_KEY=<카카오 콘솔의 REST API 키>
export APPLE_BUNDLE_ID=<iOS 앱의 번들 ID>
export GOOGLE_WEB_CLIENT_ID=<구글 클라우드 콘솔의 웹 OAuth 클라이언트 ID>
export GOOGLE_IOS_CLIENT_ID=<구글 클라우드 콘솔의 iOS OAuth 클라이언트 ID>
./gradlew bootRun
```

백엔드까지 컨테이너로 실행하려면(예: 프론트엔드 개발 환경) `.env.example`을 `.env`로 복사한 뒤 다음 명령을 사용합니다.

```bash
docker compose --profile app up --build
```

테스트는 `src/test/resources/application-test.yml`의 더미 값을 쓰므로 환경 변수 없이 실행됩니다.

```bash
./gradlew spotlessCheck test jacocoTestCoverageVerification
```

- 커버리지 리포트: `build/reports/jacoco/test/html/index.html`
- API 문서: `build/docs/asciidoc/index.html`
- Swagger UI: `./gradlew openapi3` 후 앱 실행(IDE Run 또는 `bootRun`) → http://localhost:8080/swagger-ui/index.html

## API 문서

Swagger UI 접근 주소와 프로파일별 노출 범위는 [docs/profiles.md](docs/profiles.md)를,
문서 작성 규칙은 code-convention 스킬의 [references/test-convention.md](.claude/skills/code-convention/references/test-convention.md)를, 빌드·확인 방법은 [docs/api-docs.md](docs/api-docs.md)를 참고하세요.

자세한 규칙은 [AGENTS.md](AGENTS.md)와 [docs/](docs/)를 참고하세요.
