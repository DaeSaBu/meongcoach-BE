# Back-End
DaeSaBu's Back-End Repository

## 로컬 실행

프로파일을 지정하지 않으면 `local`로 기동하며 PostgreSQL 18.3을 사용합니다. 로컬 DB는
`docker compose -f compose.local.yml up -d`로 실행하며 컨테이너가 시작될 때마다 초기화됩니다. dev/prod 프로파일은
[docs/profiles.md](docs/profiles.md)를 참고하세요.

다음 환경 변수를 설정해야 애플리케이션이 기동합니다. 기본값이 없으므로 미설정 시 기동에 실패합니다.

| 변수 | 설명 |
|---|---|
| `JWT_SECRET` | JWT 서명 키. **32바이트 이상** |
| `KAKAO_NATIVE_APP_KEY` | 네이티브 SDK가 발급한 id_token의 `aud` |
| `KAKAO_REST_API_KEY` | 웹 로그인이 발급한 id_token의 `aud` |

```bash
export JWT_SECRET=local-dev-secret-key-at-least-32-bytes-long
export KAKAO_NATIVE_APP_KEY=<카카오 콘솔의 네이티브 앱 키>
export KAKAO_REST_API_KEY=<카카오 콘솔의 REST API 키>
docker compose -f compose.local.yml up -d
./gradlew bootRun
```

백엔드까지 컨테이너로 실행하려면 `.env.example`을 `.env`로 복사한 뒤 다음 명령을 사용합니다.

```bash
docker compose -f compose.local.yml --profile app up --build
```

테스트는 `src/test/resources/application-test.yml`의 더미 값을 쓰므로 환경 변수 없이 실행됩니다.

```bash
./gradlew spotlessCheck test jacocoTestCoverageVerification
```

- 커버리지 리포트: `build/reports/jacoco/test/html/index.html`
- API 문서: `build/docs/asciidoc/index.html`

자세한 규칙은 [AGENTS.md](AGENTS.md)와 [docs/](docs/)를 참고하세요.
