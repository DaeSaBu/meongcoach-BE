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
- `./gradlew test` 실행 시 테스트 종료 후 asciidoctor가 자동 실행되어 `build/docs/asciidoc/index.html`이 생성됩니다.
- 새 API를 추가한 PR에는 해당 API의 `index.adoc` 섹션 추가를 포함합니다.

## 문서 확인 경로

- 문서는 **로컬 빌드 산출물로만 생성**되며, 배포 산출물(jar)에는 포함되지 않습니다.
- 테스트 실행 후 `build/docs/asciidoc/index.html`을 브라우저로 열어 확인합니다.
- `develop`에 merge되면 REST Docs와 Swagger UI가 [GitHub Pages 문서 사이트](https://daesabu.github.io/meongcoach-BE/)에
  자동 배포됩니다. ([docs/ci-cd.md](../ci-cd.md) 참고)

## OpenAPI 3 / Swagger UI

asciidoc 문서와 별개로, 같은 테스트에서 OpenAPI 3 스펙을 생성해 로컬 Swagger UI로 확인할 수 있습니다.

```bash
./gradlew openapi3   # test → build/api-spec/openapi3.json 생성 → bearerAuth 스킴 자동 주입
./gradlew bootRun    # http://localhost:8080/swagger-ui.html
```

- 스펙 생성: [restdocs-api-spec](https://github.com/ePages-de/restdocs-api-spec) Gradle 플러그인이
  `resource.json` 스니펫을 `build/api-spec/openapi3.json`으로 합칩니다.
- 보안 스킴: 문서화 테스트는 `principal()`로 인증을 우회하므로, `injectOpenApiSecurityScheme` 태스크가
  스펙에 bearerAuth 스킴과 전역 `security`를 주입합니다. **인증 없이 호출 가능한 공개 API를 추가하면
  `build.gradle.kts`의 `publicPaths` 목록도 함께 갱신합니다.**
- Swagger UI: springdoc은 `developmentOnly` 스코프라 로컬 bootRun에서만 서빙되고 배포 jar에는
  포함되지 않습니다. 스펙 파일도 로컬 빌드 산출물(`build/api-spec/`)에서 직접 읽습니다(`ApiDocsConfig`).
- Swagger UI의 Authorize 버튼에 액세스 토큰을 넣으면 인증이 필요한 API도 Try it out으로 호출할 수 있습니다.
