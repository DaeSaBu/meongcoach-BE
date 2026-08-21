# API 문서 산출물 (REST Docs · OpenAPI · Swagger UI)

API 문서는 Spring REST Docs로 작성합니다. 문서 스니펫은 `adapter/webapi` 테스트에서 생성되므로, **테스트를 통과한 API만 문서화됩니다.** 컨트롤러 테스트에서 문서를 작성하는 규칙은 code-convention 스킬의 [references/test-convention.md](../.claude/skills/code-convention/references/test-convention.md) "컨트롤러 테스트와 API 문서화" 절을 따릅니다. 이 문서는 산출물이 어떻게 만들어지고 어디서 확인하는지를 설명합니다.

## 문서 빌드

- 스니펫 생성 위치: `build/generated-snippets/{모듈}/{행위}/`
- `src/docs/asciidoc/index.adoc`에 모듈별 섹션(`== User API`)을 만들고 `operation::` 매크로로 스니펫을 포함합니다.
- 각 API 섹션의 `operation::` 위에는 Swagger 딥링크 한 줄을 추가합니다. 경로는 `#/{모듈 태그}/{operationId}`이며,
  모듈 태그는 `build.gradle.kts`의 `moduleTags` 값, operationId는 snippet identifier의 `/`를 `-`로 바꾼 값입니다.
  링크는 `window=swagger`로 Swagger UI 탭을 재사용하므로 새 링크도 같은 window 이름을 씁니다.

  ```adoc
  === 소셜 로그인

  link:{swagger-ui}#/Auth/user-social-login[▶ Swagger에서 Try it out,window=swagger]

  operation::user/social-login[snippets='http-request,request-fields,http-response,response-fields']
  ```
- `./gradlew test` 실행 시 테스트 종료 후 asciidoctor가 자동 실행되어 `build/docs/asciidoc/index.html`이 생성됩니다.

## 문서 확인 경로

- REST Docs HTML은 **로컬 빌드 산출물로만 생성**되며, 배포 산출물(jar)에는 포함되지 않습니다.
  테스트 실행 후 `build/docs/asciidoc/index.html`을 브라우저로 열어 확인합니다.
- Swagger UI는 API 서버가 직접 서빙합니다. 접근 주소와 프로파일별 노출 범위는 [profiles.md](profiles.md)를 참고하세요.

## OpenAPI 3 / Swagger UI

asciidoc 문서와 별개로, 같은 테스트에서 OpenAPI 3 스펙을 생성해 Swagger UI로 확인할 수 있습니다.

```bash
./gradlew openapi3   # test → build/api-spec/openapi3.json 생성 → 보안 스킴·모듈 태그 자동 후처리
./gradlew bootRun    # (또는 IDE Run) http://localhost:8080/swagger-ui/index.html
```

- 스펙 생성: [restdocs-api-spec](https://github.com/ePages-de/restdocs-api-spec) Gradle 플러그인이
  `resource.json` 스니펫을 `build/api-spec/openapi3.json`으로 합칩니다.
- 스펙 후처리: `postProcessOpenApiSpec` 태스크가 스펙을 다듬습니다.
  - 서버 주소: 상대 경로(`/`)로 덮어써 Try it out이 문서를 서빙한 오리진(localhost 또는 dev 서버)을 그대로 향합니다.
  - 보안 스킴: 문서화 테스트는 `principal()`로 인증을 우회하므로 bearerAuth 스킴과 전역 `security`를
    주입합니다. 인증 없이 호출 가능한 경로는 `build.gradle.kts`의 `publicPaths` 목록으로 제외합니다.
  - 모듈 태그: 스니펫 식별자의 모듈 접두어(`user/…`, `training/…`)를 `moduleTags` 매핑에 따라 Swagger UI 그룹
    태그(Auth, Training 등)로 바꿉니다.
  - operationId 정규화: Swagger UI 딥링크가 `/`를 해석하지 못해 `user/social-login`을
    `user-social-login`으로 바꿉니다. REST Docs의 "Try it out" 링크가 이 규칙을 사용합니다.
- Swagger UI 서빙: swagger-ui dist 정적 파일이 `src/main/resources/static/swagger-ui/`에 커밋되어
  있습니다. 스펙은 배포 jar에서는 classpath(`bootJar`가 병합), 로컬 실행에서는 `build/api-spec/`
  산출물을 읽습니다(`WebConfig`). `./gradlew openapi3`를 다시 실행하면 재시작 없이 새로고침으로 반영됩니다.
- Swagger UI의 Authorize 버튼에 액세스 토큰을 넣으면 인증이 필요한 API도 Try it out으로 호출할 수 있습니다.
