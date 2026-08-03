# RestDocs 컨벤션

API 문서는 Spring REST Docs로 작성합니다. 문서 스니펫은 `adapter/webapi` 테스트에서 생성되므로, **테스트를 통과한 API만 문서화됩니다.**

## 작성 방법

- `@WebMvcTest` + `@AutoConfigureRestDocs` 조합으로 작성하고, 테스트에 `document(...)` 호출을 포함합니다.
- snippet identifier는 `{모듈}/{행위}` 형식을 사용합니다. (예: `user/register`, `dog/register`)

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
- `./gradlew test` 실행 시 테스트 종료 후 asciidoctor가 자동 실행되어 `build/docs/asciidoc/index.html`이 생성됩니다.
- 새 API를 추가한 PR에는 해당 API의 `index.adoc` 섹션 추가를 포함합니다.

## 문서 확인 경로

- 문서는 **로컬 빌드 산출물로만 생성**되며, 배포 산출물(jar)에는 포함되지 않습니다.
- 테스트 실행 후 `build/docs/asciidoc/index.html`을 브라우저로 열어 확인합니다.
