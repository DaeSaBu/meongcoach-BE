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
- `@SpringBootTest` 전체 통합 테스트는 꼭 필요한 시나리오에만 최소한으로 사용합니다.

## 커버리지

- JaCoCo 라인 커버리지 **70% 이상**을 CI에서 검증하며, 미달 시 빌드가 실패합니다. ([docs/ci.md](../ci.md))

## 아키텍처 검증

- ArchUnit 기반으로 아키텍처를 검증하는 단위 테스트를 작성합니다. ([archunit-convention.md](archunit-convention.md))
- Spring Modulith 의존성 도입 후에는 `ApplicationModules.verify()` 모듈 경계 검증 테스트를 추가합니다. (`docs/architecture.md` 참고)
