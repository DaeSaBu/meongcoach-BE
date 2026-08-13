# 테스트 컨벤션

JUnit 5 + Spring Boot Test 기반으로 작성합니다.

## 작성 우선순위

MVP 개발 기간을 고려하여 아래 순서로 우선순위를 정해 작성합니다.

1. **Domain Unit Test** — Spring 컨텍스트 없이 도메인 로직을 검증하는 순수 단위 테스트
2. **Application Test** — `Application → Domain → DB`를 관통하는 테스트. 필요한 최소한의 컨텍스트만 사용
3. **Adapter Unit Test** — `adapter/webapi` 컨트롤러 테스트(`@WebMvcTest`). RestDocs 문서 작성을 위한 내용을 함께 포함 ([restdocs-convention.md](restdocs-convention.md))

## 위치와 구조

- 테스트 클래스는 프로덕션 코드와 동일한 패키지 구조를 `src/test/java`에 미러링해 배치합니다.
- 테스트 클래스명은 `대상클래스명 + Test`로 짓습니다. (예: `UserRegisterTest`)

## 스타일

- given-when-then 구조를 가져가되, 주석으로 명시하지 않고 빈 줄로 구간을 구분합니다.
- 테스트 클래스와 메서드에 한국어 `@DisplayName`을 붙입니다. 메서드명은 영어 camelCase로 검증 의도를 서술합니다.

```java
@Test
@DisplayName("이메일이 중복되면 가입에 실패한다")
void registerFailsWhenEmailIsDuplicated() {
	when(userRepository.existsByEmail(any())).thenReturn(true);

	assertThatThrownBy(() -> userRegisterService.register(request))
		.isInstanceOf(DuplicateEmailException.class);
}
```

## 작성 규칙

- 하나의 테스트는 하나의 동작만 검증합니다.
- 테스트 간 순서 의존성을 만들지 않습니다. 각 테스트는 독립적으로 실행 가능해야 합니다.
- 슬라이스 테스트: 컨트롤러는 `@WebMvcTest`, 리포지토리(`application/required`의 Spring Data 인터페이스)는 `@DataJpaTest`를 사용합니다.
- 외부 API 연동은 `MockRestServiceServer.bindTo(RestClient.Builder)`로 검증합니다. 어댑터가 `RestClient`가 아닌 `RestClient.Builder`를 주입받아야 이 방식이 가능하므로, 생성자 파라미터를 `Builder`로 둡니다.

## 시큐리티와 테스트 슬라이스

현재 `spring-boot-starter-security-test`를 **의존성에 넣지 않았습니다.** Spring Boot 4에서 `@WebMvcTest` 슬라이스에 시큐리티 자동설정을 넣는 것은 이 아티팩트뿐이라, 지금은 컨트롤러 슬라이스에 필터 체인이 적용되지 않습니다.

**주의:** 첫 인증 필요 엔드포인트를 테스트하려고 이 의존성을 추가하는 순간(`@WithMockUser`, `SecurityMockMvcRequestPostProcessors.jwt()` 등), `WebMvcTypeExcludeFilter`가 우리 `SecurityConfig`를 슬라이스에 포함하지 **않기** 때문에 Boot의 기본 "전부 인증" 체인이 적용되어 **기존 `@WebMvcTest`가 모두 401이 됩니다.** 그때는 순수 MVC 슬라이스에 `@AutoConfigureMockMvc(addFilters = false)`를 붙이거나 permit-all `@TestConfiguration`을 함께 도입해야 합니다.

그래서 `@CurrentUserId`가 필요한 컨트롤러 슬라이스에서는 `SecurityMockMvcRequestPostProcessors.jwt()`를 쓸 수 없습니다. 이 후처리기는 SecurityContext를 `SecurityContextRepository`에 저장할 뿐이고, 그것을 다시 요청으로 올려주는 것은 필터 체인이기 때문입니다. 대신 요청 빌더의 `.principal(...)`로 인증 주체를 직접 실어 보냅니다. `CurrentUserIdArgumentResolver`가 읽는 것이 서블릿 표준 `Principal`이라 필터 체인 없이도 그대로 해석됩니다. 인증되지 않은 상황은 `.principal(...)`을 붙이지 않는 것으로 표현합니다.

```java
private static final Principal CURRENT_USER = () -> "42";

@Test
@DisplayName("인증 주체에서 읽은 사용자로 조회를 위임한다")
void findDelegatesWithCurrentUserId() throws Exception {
	mockMvc.perform(get("/api/training/curriculums").principal(CURRENT_USER)) ...
}
```

필터 체인 자체의 동작(`SecurityConfig`는 커버리지 검증 제외 대상)은 `shared/config/SecurityFilterChainTest`가 `@SpringBootTest`로 실제 요청을 보내 검증합니다.
- `@SpringBootTest` 전체 통합 테스트는 꼭 필요한 시나리오에만 최소한으로 사용합니다.

## 커버리지

- JaCoCo 라인 커버리지 **70% 이상**을 CI에서 검증하며, 미달 시 빌드가 실패합니다. ([docs/ci-cd.md](../ci-cd.md))

## 아키텍처 검증

- ArchUnit 기반으로 아키텍처를 검증하는 단위 테스트를 작성합니다. ([archunit-convention.md](archunit-convention.md))
- 모듈 경계는 `architecture/ModularityTest`의 `ApplicationModules.verify()`가 검증합니다. (`docs/architecture.md` 참고)
