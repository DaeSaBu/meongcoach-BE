# RestDocs 컨벤션

API 문서는 Spring REST Docs로 작성합니다. 문서 스니펫은 `adapter/webapi` 테스트에서 생성되므로, **테스트를 통과한 API만 문서화됩니다.**

## 작성 방법

- `@WebMvcTest` + `@AutoConfigureRestDocs` 조합으로 작성하고, 테스트에 `document(...)` 호출을 포함합니다.
- snippet identifier는 `{모듈}/{행위}` 형식을 사용합니다. (예: `user/register`, `walk/start`)

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
- `./gradlew asciidoctor` 실행 시 `build/docs/asciidoc/index.html`이 생성됩니다. (test → asciidoctor 순으로 자동 실행)
- 새 API를 추가한 PR에는 해당 API의 `index.adoc` 섹션 추가를 포함합니다.
