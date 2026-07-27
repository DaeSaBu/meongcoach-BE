# Back-End
DaeSaBu's Back-End Repository

## 로컬 실행

다음 환경 변수를 설정해야 애플리케이션이 기동합니다. 기본값이 없으므로 미설정 시 기동에 실패합니다.

| 변수 | 설명 |
|---|---|
| `JWT_SECRET` | JWT 서명 키. **32바이트 이상** |
| `KAKAO_APP_ID` | 카카오 개발자 콘솔의 앱 ID(숫자) |

```bash
export JWT_SECRET=local-dev-secret-key-at-least-32-bytes-long
export KAKAO_APP_ID=<카카오 콘솔의 앱 ID>
./gradlew bootRun
```

테스트는 `src/test/resources/application-test.yml`의 더미 값을 쓰므로 환경 변수 없이 실행됩니다.

```bash
./gradlew spotlessCheck test jacocoTestCoverageVerification
```

- 커버리지 리포트: `build/reports/jacoco/test/html/index.html`
- API 문서: `build/docs/asciidoc/index.html`

자세한 규칙은 [AGENTS.md](AGENTS.md)와 [docs/](docs/)를 참고하세요.
