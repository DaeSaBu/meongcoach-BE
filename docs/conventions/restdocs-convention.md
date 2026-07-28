# RestDocs 컨벤션

API 문서는 Spring REST Docs로 작성합니다. 문서 스니펫은 `adapter/webapi` 테스트에서 생성되므로, **테스트를 통과한 API만 문서화됩니다.**

향후 동일한 스니펫을 OpenAPI 3 스펙과 Swagger UI로도 제공할 예정입니다. 아래 규칙은 그때 **테스트 코드를 고치지 않아도 되도록** 정해 둔 것이므로, 번거롭더라도 생략하지 않습니다.

## 작성 방법

- `@WebMvcTest(대상Controller.class)` + `@AutoConfigureRestDocs` 조합으로 작성합니다.
- `document(...)`는 **정적 임포트해서 클래스명 없이 호출**합니다. 문서화 도구를 교체할 때 임포트 한 줄만 바꾸면 되도록 하기 위함입니다.
- 하나의 API는 **한 번의 `document(...)` 호출에 모든 스니펫을 전달**합니다. 공통 설정(`alwaysDo`)이나 헬퍼 클래스에서 스니펫을 나중에 덧붙이는 방식은 쓰지 않습니다. 호출 시점에 스니펫이 모두 모여 있어야 필드 정보가 온전히 수집됩니다.
- 요청은 `RestDocumentationRequestBuilders`로 만들고, **경로 변수는 실제 값이 아니라 URI 템플릿으로 넘깁니다.** 리터럴 경로(`get("/api/dogs/1")`)로 요청하면 경로 변수를 문서화할 수 없고, 문서상 경로도 `/api/dogs/1`로 굳어집니다.

```java
// import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
// import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;

@WebMvcTest(UserController.class)
@AutoConfigureRestDocs
@DisplayName("회원 API")
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("회원 가입에 성공하면 생성된 회원 ID를 반환한다")
	void registerReturnsCreatedUserId() throws Exception {
		mockMvc.perform(post("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated())
			.andDo(document("user/register",
				requestFields(
					fieldWithPath("email").description("로그인에 사용할 이메일. 중복 불가"),
					fieldWithPath("nickname").description("다른 회원에게 보이는 이름. 2~10자")
				),
				responseFields(
					fieldWithPath("id").description("생성된 회원 ID"),
					fieldWithPath("nickname").description("등록된 닉네임"),
					fieldWithPath("profileImageUrl").type(JsonFieldType.STRING).optional()
						.description("프로필 이미지 URL. 등록 직후에는 null")
				)
			));
	}
}
```

## 스니펫 식별자

- 형식은 `{모듈}/{행위}`이며 전부 소문자 kebab-case로 씁니다. (`user/register`, `dog/update-profile`)
- `{모듈}`은 Modulith 모듈의 최상위 패키지명과 **똑같이** 씁니다. 문서의 그룹(향후 OpenAPI tag)이 되는 값이라 임의로 줄이거나 바꾸지 않습니다.
- `{행위}`는 유스케이스를 나타내는 동사구로 짓습니다. HTTP 메서드나 URL을 그대로 옮기지 않습니다. (`user/post-users` ✗ → `user/register` ✓)
- 실패 케이스는 `{모듈}/{행위}-error-{원인}` 형식을 씁니다. (`user/register-error-duplicate-email`) 한 API에 실패 케이스가 여러 개여도 서로 덮어쓰지 않습니다.
- **merge된 식별자는 이름을 바꾸지 않습니다.** 식별자는 문서 참조 키이자 향후 operationId가 되므로, 변경하면 문서 링크와 이를 기반으로 생성된 클라이언트 코드가 함께 깨집니다.
- 특정 모듈에 속하지 않는 공통 문서에만 `shared/`를 씁니다. (`shared/error`)

## 문서화 필수 항목

| 대상 | 스니펫 | 규칙 |
|------|--------|------|
| 경로 변수 | `pathParameters` | 있으면 전부 |
| 쿼리 파라미터 | `queryParameters` | 있으면 전부 |
| 요청 본문 | `requestFields` | 있으면 전부 |
| 요청 헤더 | `requestHeaders` | 인증 등 클라이언트가 직접 넣어야 하는 헤더만 |
| 응답 본문 | `responseFields` | 필수, 전부 |
| 실패 응답 | `responseFields` | 대표 케이스 1개 이상 |

- 하나의 `경로 + HTTP 메서드 + 상태 코드` 조합은 **대표 테스트 하나에서만 전체 필드를 문서화**합니다. 같은 조합을 여러 테스트에서 문서화하면 하나의 스펙으로 합쳐질 때 서로 덮어써서, 어느 쪽이 남을지 예측할 수 없습니다.
- 조회 조건이나 응답 형태가 다른 케이스는 별도 문서로 만들지 말고, 대표 테스트의 필드 설명에 조건을 적습니다.

## 필드 작성 규칙

- 항상 존재하지는 않는 필드에는 `.optional()`을 붙입니다. 필수/선택 구분이 그대로 스키마의 required 여부가 됩니다.
- 예시 값이 `null`일 수 있는 필드는 `.type(JsonFieldType.STRING)`처럼 **타입을 명시**합니다. 값으로 타입을 추론할 수 없으면 문서화가 실패하고, 통과하더라도 스키마에 타입이 비게 됩니다.
- 중첩 객체와 배열은 `fieldWithPath("items[].name")`처럼 **끝까지 펼쳐서** 문서화합니다. `subsectionWithPath`는 내부 구조를 통째로 감추므로, 구조가 실제로 가변적인 경우에만 씁니다.
- 테스트 픽스처는 모든 선택 필드가 채워진 응답을 만들도록 구성합니다. 필드가 비어 있으면 그 필드는 문서에서 누락됩니다.

## 설명(description) 작성

- 한국어로, 필드마다 한 문장으로 씁니다.
- 필드명을 한국어로 옮기기만 한 설명은 정보가 없습니다. 그 값이 무엇이고 어떻게 쓰이는지 적습니다. (`nickname` → "닉네임" ✗ → "다른 회원에게 보이는 이름. 2~10자" ✓)
- **제약 조건을 설명에 함께 적습니다.** 형식(`ISO-8601`), 범위(`1 이상`), 열거값(`UP` 또는 `DOWN`), 단위(`초`), 기본값은 스키마로 표현되지 않으므로 설명이 유일한 전달 수단입니다.
- 표기는 **Markdown과 AsciiDoc 양쪽에서 깨지지 않는 것만** 씁니다. 인라인 코드(`` ` ``)까지는 허용하고, AsciiDoc 전용 매크로(`link:`, `<<...>>`, `+++`)나 설명 안의 줄바꿈은 쓰지 않습니다.
- 빈 설명(`description("")`)을 두지 않습니다.

## 인증 헤더

인증이 필요한 API는 아래 문구를 그대로 사용해 문서화합니다. 표현이 일정해야 나중에 문서 전체의 인증 스키마로 한 번에 승격시킬 수 있습니다.

```java
requestHeaders(
	headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer 액세스 토큰")
)
```

- 인증 토큰을 `requestFields`나 쿼리 파라미터로 문서화하지 않습니다.

## 실패 응답 문서화

- 공통 Problem Details 형식은 `shared/error`, `shared/error-validation`에 한 번만 문서화되어 있습니다. ([exception-convention.md](exception-convention.md))
- 각 API는 그와 별개로 **자기 식별자로 대표 실패 케이스를 1개 이상** 문서화합니다. 공통 문서만 있으면 그 API가 어떤 상태 코드와 `code`를 돌려주는지 알 수 없습니다.
- 실패 응답 테스트는 상태 코드와 `$.code`를 함께 검증합니다.

```java
mockMvc.perform(post("/api/users")
		.contentType(MediaType.APPLICATION_JSON)
		.content(duplicateEmailRequest))
	.andExpect(status().isConflict())
	.andExpect(jsonPath("$.code").value("USER_DUPLICATE_EMAIL"))
	.andDo(document("user/register-error-duplicate-email",
		responseFields(
			fieldWithPath("title").description("HTTP 상태 이름"),
			fieldWithPath("status").description("HTTP 상태 코드"),
			fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
			fieldWithPath("instance").description("에러가 발생한 요청 경로"),
			fieldWithPath("code").description("클라이언트 분기용 에러 코드. `USER_DUPLICATE_EMAIL`"),
			fieldWithPath("timestamp").description("에러 발생 시각. UTC ISO-8601")
		)
	));
```

## 문서 빌드

- 스니펫 생성 위치: `build/generated-snippets/{모듈}/{행위}/`
- `src/docs/asciidoc/index.adoc`에 모듈별 섹션(`== User API`)을 만들고 `operation::` 매크로로 스니펫을 포함합니다.
- 섹션 제목은 API 한 줄 요약으로 짓습니다. (`=== 회원 가입`) 향후 스펙의 요약 문구로 재사용할 값이므로, `=== register` 같은 식별자 나열은 쓰지 않습니다.
- `operation::`의 `snippets=` 목록에는 해당 테스트에서 문서화한 스니펫을 빠짐없이 적습니다.
- `./gradlew test` 실행 시 테스트 종료 후 asciidoctor가 자동 실행되어 `build/docs/asciidoc/index.html`이 생성됩니다.
- 새 API를 추가한 PR에는 해당 API의 `index.adoc` 섹션 추가를 포함합니다.

## 문서 확인 경로

- 문서는 **로컬 빌드 산출물로만 생성**되며, 배포 산출물(jar)에는 포함되지 않습니다.
- 테스트 실행 후 `build/docs/asciidoc/index.html`을 브라우저로 열어 확인합니다.

## 자동 검증

위 규칙 중 정적으로 판단할 수 있는 것은 `architecture/RestDocsConventionTest`가 테스트 소스를 읽어 검증합니다. 스니펫 식별자와 필드 설명은 문자열 리터럴이라 바이트코드에 남지 않으므로 ArchUnit이 아니라 소스 스캔 방식을 씁니다.

검증 항목은 다음 아홉 가지이며, 위반하면 어느 파일의 어떤 식별자가 문제인지 실패 메시지에 나옵니다.

- `@WebMvcTest`와 `@AutoConfigureRestDocs`를 함께 선언했는지
- `document(...)`를 정적 임포트해 클래스명 없이 호출했는지
- 요청을 `RestDocumentationRequestBuilders`로 만들었는지
- 식별자가 `{모듈}/{행위}` 소문자 kebab-case 형식인지
- 식별자의 모듈 세그먼트가 실제 모듈 패키지명인지
- 식별자가 저장소 전체에서 유일한지
- 경로 변수를 쓰면서 `pathParameters`를 빠뜨리지 않았는지
- 빈 `description("")`이 없는지
- 문서화한 스니펫과 `index.adoc`의 `operation::` 매크로가 서로 일치하는지

`.optional()`·`.type()` 누락과 설명의 품질은 응답 값에 따라 달라져 정적으로 판단할 수 없으므로 리뷰에서 확인합니다.

## PR 전 체크리스트

- [ ] 식별자가 `{모듈}/{행위}` 형식이고, 기존 식별자를 바꾸지 않았다
- [ ] 경로 변수를 URI 템플릿으로 넘기고 `pathParameters`로 문서화했다
- [ ] 요청·응답의 모든 필드를 문서화했고, 선택 필드에 `.optional()`을 붙였다
- [ ] `null`일 수 있는 필드에 `.type(...)`을 명시했다
- [ ] 설명에 제약 조건(형식·범위·열거값·단위)이 들어 있다
- [ ] 대표 실패 케이스를 자기 식별자로 문서화했다
- [ ] `index.adoc`에 섹션을 추가했다
