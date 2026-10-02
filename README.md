# Back-End
DaeSaBu's Back-End Repository

## 로컬 실행

프로파일을 지정하지 않으면 `local`로 기동합니다. 로컬 전용 DB는 없으며, bastion 호스트로 dev DB에 SSH 터널을 연 뒤
`DB_*` 환경 변수를 터널 주소로 설정해 접속합니다. 프로파일별 구성은 [docs/profiles.md](docs/profiles.md)를 참고하세요.

다음 환경 변수를 설정해야 애플리케이션이 기동합니다. 각 변수의 의미와 제약은
[docs/security.md](docs/security.md#환경-변수)를 참고하세요.

```bash
export JWT_SECRET=local-dev-secret-key-at-least-32-bytes-long
export KAKAO_NATIVE_APP_KEY=<카카오 콘솔의 네이티브 앱 키>
export KAKAO_REST_API_KEY=<카카오 콘솔의 REST API 키>
export APPLE_BUNDLE_ID=<iOS 앱의 번들 ID>
export APPLE_TEAM_ID=<Apple Developer 팀 ID>
export APPLE_KEY_ID=<Sign in with Apple 키 ID>
export APPLE_PRIVATE_KEY="$(cat AuthKey_XXXXXXXXXX.p8)"
export GOOGLE_WEB_CLIENT_ID=<구글 클라우드 콘솔의 웹 OAuth 클라이언트 ID>
export GOOGLE_IOS_CLIENT_ID=<구글 클라우드 콘솔의 iOS OAuth 클라이언트 ID>
export DB_HOST=localhost DB_NAME=<dev DB 이름> DB_USERNAME=<dev DB 사용자> DB_PASSWORD=<dev DB 비밀번호>
./gradlew bootRun
```

테스트는 `src/test/resources/application-test.yml`의 더미 값을 쓰므로 환경 변수 없이 실행됩니다. 단, DB 테스트가 Testcontainers로 PostgreSQL 컨테이너를 띄우므로 Docker 데몬이 실행 중이어야 합니다.

```bash
./gradlew spotlessCheck test jacocoTestCoverageVerification
```

- 커버리지 리포트: `build/reports/jacoco/test/html/index.html`
- API 문서: `build/docs/asciidoc/index.html`
- Swagger UI: `./gradlew openapi3` 후 앱 실행(IDE Run 또는 `bootRun`) → http://localhost:8080/swagger-ui/index.html

## API 문서

Swagger UI 접근 주소와 프로파일별 노출 범위는 [docs/profiles.md](docs/profiles.md)를,
문서 작성 규칙은 code-convention 스킬의 [references/test-convention.md](.claude/skills/code-convention/references/test-convention.md)를, 빌드·확인 방법은 [docs/api-docs.md](docs/api-docs.md)를 참고하세요.

자세한 규칙은 [AGENTS.md](AGENTS.md)를 참고하세요.
