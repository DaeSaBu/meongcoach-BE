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
- 인증이 필요한 API의 요청 예시: `Authorization: Bearer access-token` 헤더
- 응답: response fields
- 실패 응답: 대표 에러 케이스 1개 이상 (Problem Details 형식)

`@WebMvcTest`에서 `.principal(...)`은 인증 주체만 주입하고 HTTP 헤더를 만들지 않습니다. 문서 스니펫을 생성하는
요청에는 `.principal(...)`과 별도로 위 인증 헤더를 추가합니다.

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

- REST Docs HTML은 **로컬 빌드 산출물로만 생성**되며, 배포 산출물(jar)에는 포함되지 않습니다.
  테스트 실행 후 `build/docs/asciidoc/index.html`을 브라우저로 열어 확인합니다.
- Swagger UI는 API 서버가 직접 서빙합니다. 접근 주소와 프로파일별 노출 범위는 [docs/profiles.md](../profiles.md)를 참고하세요.

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
    주입합니다. **인증 없이 호출 가능한 공개 API를 추가하면 `build.gradle.kts`의 `publicPaths` 목록도
    함께 갱신합니다.**
  - 모듈 태그: 스니펫 식별자의 모듈 접두어(`user/…`, `training/…`)를 Swagger UI 그룹 태그(Auth,
    Training 등)로 바꿉니다. **새 모듈을 추가하면 `moduleTags` 매핑도 함께 갱신합니다.**
  - operationId 정규화: Swagger UI 딥링크가 `/`를 해석하지 못해 `user/social-login`을
    `user-social-login`으로 바꿉니다. REST Docs의 "Try it out" 링크가 이 규칙을 사용합니다.
  - "Try it out" 링크는 `window=swagger`로 Swagger UI 탭을 재사용합니다. 새 링크를 추가할 때도
    같은 window 이름을 사용합니다.
- Swagger UI 서빙: swagger-ui dist 정적 파일이 `src/main/resources/static/swagger-ui/`에 커밋되어
  있습니다. 스펙은 배포 jar에서는 classpath(`bootJar`가 병합), 로컬 실행에서는 `build/api-spec/`
  산출물을 읽습니다(`WebConfig`). `./gradlew openapi3`를 다시 실행하면 재시작 없이 새로고침으로 반영됩니다.
- Swagger UI의 Authorize 버튼에 액세스 토큰을 넣으면 인증이 필요한 API도 Try it out으로 호출할 수 있습니다.
