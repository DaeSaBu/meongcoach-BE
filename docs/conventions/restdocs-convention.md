# RestDocs 컨벤션

API 문서는 Spring REST Docs로 작성합니다. 문서 스니펫은 `adapter/webapi` 테스트에서 생성되므로, **테스트를 통과한 API만 문서화됩니다.**

## 작성 방법

- `@WebMvcTest` + `@AutoConfigureRestDocs` 조합으로 작성하고, 테스트에 `document(...)` 호출을 포함합니다.
- snippet identifier는 `{모듈}/{행위}` 형식을 사용합니다. (예: `user/register`, `dog/register`)
- `document`는 `com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document`를 static import
  합니다. 기존 스니펫에 더해 OpenAPI 스펙의 재료인 `resource.json`이 함께 생성됩니다.
  (예외: `GlobalExceptionHandlerTest`는 테스트 전용 `/test/**` 경로가 스펙에 섞이지 않도록
  기존 `MockMvcRestDocumentation.document`를 유지합니다.)

```java
@WebMvcTest(UserController.class)
@AutoConfigureRestDocs
class UserControllerTest {

	@Test
	void register() throws Exception {
		mockMvc.perform(post("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated())
			.andDo(document("user/register",
				requestFields(
					fieldWithPath("email").description("이메일"),
					fieldWithPath("nickname").description("닉네임")
				),
				responseFields(
					fieldWithPath("id").description("회원 ID")
				)
			));
	}
}
```

## 문서화 필수 항목

- 요청: path parameters / query parameters / request fields (해당하는 것 모두)
- 응답: response fields
- 실패 응답: 대표 에러 케이스 1개 이상 (Problem Details 형식)

## 문서 빌드

- 스니펫 생성 위치: `build/generated-snippets/{모듈}/{행위}/`
- `src/docs/asciidoc/index.adoc`에 모듈별 섹션(`== User API`)을 만들고 `operation::` 매크로로 스니펫을 포함합니다.
- 각 API 섹션의 `operation::` 위에는 Swagger 딥링크 한 줄을 추가합니다. 경로는 `#/{모듈 태그}/{operationId}`이며,
  모듈 태그는 `build.gradle.kts`의 `moduleTags` 값, operationId는 snippet identifier의 `/`를 `-`로 바꾼 값입니다.

  ```adoc
  === 소셜 로그인

  link:{swagger-ui}#/Auth/user-social-login[▶ Swagger에서 Try it out,window=swagger]

  operation::user/social-login[snippets='http-request,request-fields,http-response,response-fields']
  ```
- `./gradlew test` 실행 시 테스트 종료 후 asciidoctor가 자동 실행되어 `build/docs/asciidoc/index.html`이 생성됩니다.
- 새 API를 추가한 PR에는 해당 API의 `index.adoc` 섹션 추가를 포함합니다.

## 문서 확인 경로

- 문서는 **로컬 빌드 산출물로만 생성**되며, 배포 산출물(jar)에는 포함되지 않습니다.
- 테스트 실행 후 `build/docs/asciidoc/index.html`을 브라우저로 열어 확인합니다.
- `develop`에 merge되어 dev 배포가 성공하면 REST Docs와 Swagger UI가
  [GitHub Pages 문서 사이트](https://daesabu.github.io/meongcoach-BE/)에 자동 배포됩니다.
  랜딩에서 두 문서를 한 화면에 나란히 보여줍니다. ([docs/ci-cd.md](../ci-cd.md) 참고)

## OpenAPI 3 / Swagger UI

asciidoc 문서와 별개로, 같은 테스트에서 OpenAPI 3 스펙을 생성해 로컬 Swagger UI로 확인할 수 있습니다.

```bash
./gradlew openapi3   # test → build/api-spec/openapi3.json 생성 → 보안 스킴·모듈 태그 자동 후처리
./gradlew bootRun    # http://localhost:8080/swagger-ui.html
```

- 스펙 생성: [restdocs-api-spec](https://github.com/ePages-de/restdocs-api-spec) Gradle 플러그인이
  `resource.json` 스니펫을 `build/api-spec/openapi3.json`으로 합칩니다.
- 스펙 후처리: `postProcessOpenApiSpec` 태스크가 스펙을 다듬습니다.
  - 보안 스킴: 문서화 테스트는 `principal()`로 인증을 우회하므로 bearerAuth 스킴과 전역 `security`를
    주입합니다. **인증 없이 호출 가능한 공개 API를 추가하면 `build.gradle.kts`의 `publicPaths` 목록도
    함께 갱신합니다.**
  - 모듈 태그: 스니펫 식별자의 모듈 접두어(`user/…`, `training/…`)를 Swagger UI 그룹 태그(Auth,
    Training 등)로 바꿉니다. **새 모듈을 추가하면 `moduleTags` 매핑도 함께 갱신합니다.**
  - operationId 정규화: Swagger UI 딥링크가 `/`를 해석하지 못해 `user/social-login`을
    `user-social-login`으로 바꿉니다. REST Docs의 "Try it out" 링크가 이 규칙을 사용합니다.
  - "Try it out" 링크는 `window=swagger`로 문서 사이트 랜딩의 Swagger iframe(`name="swagger"`,
    `src/docs/site/index.html`)을 갱신합니다. 새 링크를 추가할 때도 같은 window 이름을 사용합니다.
- Swagger UI: springdoc은 `developmentOnly` 스코프라 로컬 bootRun에서만 서빙되고 배포 jar에는
  포함되지 않습니다. 스펙 파일도 로컬 빌드 산출물(`build/api-spec/`)에서 직접 읽습니다(`ApiDocsConfig`).
  프로파일별 springdoc 설정은 [docs/profiles.md](../profiles.md)를 참고하세요.
- Swagger UI의 Authorize 버튼에 액세스 토큰을 넣으면 인증이 필요한 API도 Try it out으로 호출할 수 있습니다.
