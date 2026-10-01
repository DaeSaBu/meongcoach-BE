# 테스트 컨벤션

JUnit 5 + Spring Boot Test 기반으로 작성한다. 테스트 코드도 [SKILL.md](../SKILL.md)의 코드 스타일 규칙을 그대로 따른다.

## 작성 우선순위

MVP 개발 기간을 고려하여 아래 순서로 우선순위를 정해 작성한다.

1. **Domain Unit Test** — Spring 컨텍스트 없이 도메인 로직을 검증하는 순수 단위 테스트
2. **Application Test** — `@SpringBootTest` + `@Transactional` 통합 테스트로 `provided` 인터페이스 → 서비스 → Domain → DB를 관통해 검증 ([Application 테스트](#application-테스트))
3. **Adapter Unit Test** — `adapter/webapi` 컨트롤러 테스트(`@WebMvcTest`). RestDocs 문서 작성을 위한 내용을 함께 포함 ([컨트롤러 테스트와 API 문서화](#컨트롤러-테스트와-api-문서화))

## 작성하지 않는 대상

선언을 그대로 되읽기만 하는 테스트는 결함을 잡지 못하고 상수 하나 추가할 때마다 깨지므로 만들지 않는다.

- **단순 enum** — 상수 나열, 필드 getter, 문자열→상수 조회(`valueOf` 래핑·필드 매칭)와 그 실패 시 도메인 예외 변환만 있는 enum은 단위 테스트를 만들지 않는다. (예: `user/domain/Gender.from`, `media/domain/ImageType.fromContentType`) enum은 JaCoCo 커버리지 대상에서 이미 제외되어(`build.gradle.kts`의 `isEnumClassFile`) 커버리지 명분도 없고, 변환 실패 경로는 그 enum을 받는 엔티티·서비스·컨트롤러 테스트의 에러 케이스에서 간접 검증된다. 분기·계산 로직이 있는 메서드는 **그 메서드만** 테스트한다. (예: `training/domain/CurriculumStatus.of(int, int)`)
- **`{모듈}ErrorCode`** — 전용 테스트 클래스(`~ErrorCodeTest`)를 만들지 않는다. `code()`가 `name()`을 반환하는 것은 `shared/exception/ErrorCode` 계약이고 상태·메시지는 데이터 선언이다. 에러 코드가 실제로 어떤 응답이 되는지는 컨트롤러 테스트의 `{모듈}/{행위}-error` 문서화 케이스가 검증한다. ([컨트롤러 테스트와 API 문서화](#컨트롤러-테스트와-api-문서화))

## 스타일

- given-when-then 구조를 가져가되, 주석으로 명시하지 않고 빈 줄로 구간을 구분한다.
- `@DisplayName`은 쓰지 않는다. 테스트 메서드명을 한국어 문장으로 적어 검증 의도를 서술하고, 단어는 `_`로 구분한다. 문장 안에 등장하는 식별자(`contentType`, `null`, 클래스명)는 원래 표기를 유지한다. (예: `강아지가_5마리면_더_등록할_수_없다()`, `요청한_contentType_그대로_스토리지에_전달한다()`) 테스트 클래스명은 `{대상}Test`로 두고 클래스 레벨 `@DisplayName`도 붙이지 않는다.
	- `src/test/resources/junit-platform.properties`의 `ReplaceUnderscores` 표시 이름 생성기가 리포트·IDE에서 `_`를 공백으로 바꿔 보여준다. 인자별 표시 이름이 필요하면 `@ParameterizedTest(name = ...)`에서만 지정한다.

## 작성 규칙

- 동작을 추가·변경할 때는 테스트를 먼저 작성해 `test` 커밋으로 올리고, 구현은 뒤따르는 커밋으로 올린다. 커밋 분리 규칙은 git-convention 스킬의 "커밋 단위" 참고.
- 슬라이스 테스트: 컨트롤러는 `@WebMvcTest`, 리포지토리(`application/required`의 Spring Data 인터페이스)는 `@DataJpaTest`를 사용한다.
- 운영 코드에 생성 경로가 없는 엔티티(시드 SQL로만 적재하는 엔티티)는 테스트 소스의 같은 패키지에 `{엔티티}Fixture`를 두고 `ReflectionTestUtils.setField`로 만든다. 테스트에서 쓰려고 운영 코드에 정적 팩토리나 Command를 추가하지 않는다. (살아있는 예시: `auth/domain/EmailAccountFixture`, `training/domain/TopicFixture`)
- 테스트 DB는 `src/test/resources/application-test.yml`의 `jdbc:tc:` URL로 Testcontainers가 띄우는 PostgreSQL 컨테이너 하나를 테스트 JVM 전체가 공유한다. `@DataJpaTest`에 `@AutoConfigureTestDatabase`를 붙이지 않는다 — Spring Boot는 `jdbc:tc:` URL로 잡힌 DataSource를 임베디드 DB로 교체하지 않는다. 다른 컨텍스트의 `create-drop`과 섞이면 안 되는 스키마 검증은 `migration/FlywaySchemaValidationTest`처럼 DB 이름이 다른 `jdbc:tc:` URL을 지정해 별도 컨테이너를 쓴다.
- Application 테스트 외의 `@SpringBootTest`(시큐리티 필터 체인·CORS 등)는 꼭 필요한 시나리오에만 쓴다.
- 외부 API 연동은 `MockRestServiceServer.bindTo(RestClient.Builder)`로 검증한다. 어댑터가 `RestClient`가 아닌 `RestClient.Builder`를 주입받아야 이 방식이 가능하므로, 생성자 파라미터를 `Builder`로 둔다.

## Application 테스트

- `@SpringBootTest` + `@Transactional`로 작성한다. 각 테스트는 끝나면 롤백된다.
- 테스트 대상은 `application/provided` 인터페이스로 `@Autowired` 주입한다. 서비스 구현 클래스를 주입하거나 `new`로 조립하지 않는다. 테스트 클래스명은 `{provided 인터페이스}Test`로 둔다. (예: `EntitlementSynchronizerTest`)
- provided 인터페이스가 없는 내부 서비스(`EntitlementModifyService`, `UserQueryService` 등)는 전용 테스트를 만들지 않고, 그 서비스를 호출하는 provided 인터페이스의 테스트에서 검증한다.
- 픽스처 준비와 결과 확인은 `application/required` 리포지토리를 주입받아 한다.
- 외부 연동 포트(`adapter/integration`이 구현하는 `application/required` 인터페이스 — RevenueCat·소셜 제공자·스토리지 등)는 공통 테스트 설정 한 곳에서 `@MockitoBean`으로 대체한다. 테스트 클래스마다 `@MockitoBean`·`@TestPropertySource`·`@ActiveProfiles`를 따로 선언하지 않는다.
	- 목 구성이나 프로퍼티가 클래스마다 다르면 스프링이 컨텍스트를 새로 띄운다. 캐시된 컨텍스트마다 Hikari 풀(최대 2)이 살아 있어, 컨텍스트가 늘면 PostgreSQL 커넥션 한도에 걸려 테스트가 멈춘다.
	- 공통 설정은 `support/ApplicationTest` 메타 어노테이션(`@SpringBootTest` + `@Transactional` + 외부 연동 포트 `@MockitoBean(types = ...)`)이다. Application 테스트 클래스에는 이 어노테이션 하나만 붙이고, 모듈을 옮기며 새 외부 연동 포트가 필요하면 `types`에 추가한다. `NonTransactionalApplicationTest`의 `types`에도 같이 추가한다. (살아있는 예시: `entitlement/application/EntitlementSynchronizerTest`)
	- 스텁은 테스트 안에서 `given(...)`으로 지정한다. `@MockitoBean`은 테스트마다 초기화된다.
- 트랜잭션 커밋 이후에만 드러나는 동작(advisory lock 대기·해제, `@TransactionalEventListener(AFTER_COMMIT)`, `REQUIRES_NEW`로 분리한 쓰기)은 테스트 롤백으로는 보이지 않는다. 이런 테스트는 `@ApplicationTest` 대신 `support/NonTransactionalApplicationTest`를 붙이고, 필요하면 `TransactionTemplate`으로 트랜잭션을 직접 커밋한 뒤 만든 데이터를 `@AfterEach`에서 지운다. 클래스명은 `{provided 인터페이스}{상황}Test`로 둔다. (살아있는 예시: `entitlement/application/EntitlementSynchronizerConcurrencyTest`)
	- `NonTransactionalApplicationTest`는 `@ApplicationTest`에서 `@Transactional`만 뺀 설정이다. `@MockitoBean(types = ...)`를 두 어노테이션에 똑같이 유지해야 컨텍스트를 공유한다.
	- 워커 스레드에서 부른 서비스는 테스트 트랜잭션에 참여하지 않고 실제로 커밋하므로 `@ApplicationTest`에서 스레드를 띄우면 데이터가 남는다. 또 테스트 트랜잭션이 풀(최대 2)의 커넥션 하나를 쥐어 워커가 락이 아니라 커넥션을 기다리게 되므로, 락이 없어도 통과한다.
	- 커넥션을 쥐는 스레드는 동시에 2개까지만 둔다. 3개부터는 풀 대기로 직렬화되어 경합이 재현되지 않는다.
	- 동시 실행 결과는 타이밍에 따라 달라지므로 스레드를 경쟁시키지 않는다. 테스트가 먼저 락을 쥐고 대상 호출이 그동안 끝나지 않는지(`Future.get(timeout)`이 `TimeoutException`) 확인한 뒤 락을 푼다. 작성 후 락 호출을 지우면 테스트가 실패하는지 확인한다.
- 이 규칙 이전에 작성한 Application 테스트(`@DataJpaTest` + `@Import`, 구현체 직접 조립)는 일괄 전환하지 않고 해당 테스트를 수정할 때 옮긴다.

## 시큐리티와 테스트 슬라이스

현재 `spring-boot-starter-security-test`를 **의존성에 넣지 않았다.** Spring Boot 4에서 `@WebMvcTest` 슬라이스에 시큐리티 자동설정을 넣는 것은 이 아티팩트뿐이라, 지금은 컨트롤러 슬라이스에 필터 체인이 적용되지 않는다.

**주의:** 첫 인증 필요 엔드포인트를 테스트하려고 이 의존성을 추가하는 순간(`@WithMockUser`, `SecurityMockMvcRequestPostProcessors.jwt()` 등), `WebMvcTypeExcludeFilter`가 우리 `SecurityConfig`를 슬라이스에 포함하지 **않기** 때문에 Boot의 기본 "전부 인증" 체인이 적용되어 **기존 `@WebMvcTest`가 모두 401이 된다.** 그때는 순수 MVC 슬라이스에 `@AutoConfigureMockMvc(addFilters = false)`를 붙이거나 permit-all `@TestConfiguration`을 함께 도입해야 한다.

그래서 `@CurrentUserId`가 필요한 컨트롤러 슬라이스에서는 `SecurityMockMvcRequestPostProcessors.jwt()`를 쓸 수 없다. 이 후처리기는 SecurityContext를 `SecurityContextRepository`에 저장할 뿐이고, 그것을 다시 요청으로 올려주는 것은 필터 체인이기 때문이다. 대신 요청 빌더의 `.principal(...)`로 인증 주체를 직접 실어 보낸다. `CurrentUserIdArgumentResolver`가 읽는 것이 서블릿 표준 `Principal`이라 필터 체인 없이도 그대로 해석된다. 인증되지 않은 상황은 `.principal(...)`을 붙이지 않는 것으로 표현한다.

```java
private static final Principal CURRENT_USER = () -> "42";

@Test
void 인증_주체에서_읽은_사용자로_조회를_위임한다() throws Exception {
	mockMvc.perform(get("/api/training/curriculums").principal(CURRENT_USER)) ...
}
```

필터 체인 자체의 동작(`SecurityConfig`는 커버리지 검증 제외 대상)은 `shared/config/SecurityFilterChainTest`가 `@SpringBootTest`로 실제 요청을 보내 검증한다.

## 컨트롤러 테스트와 API 문서화

API 문서는 컨트롤러 테스트가 생성하는 Spring REST Docs 스니펫으로 만든다. 산출물 빌드·확인 방법은 [docs/api-docs.md](../../../../docs/api-docs.md)를 본다. 살아있는 예시는 `auth/adapter/webapi/AuthControllerTest`.

- `@WebMvcTest` + `@AutoConfigureRestDocs` 조합으로 작성하고, 테스트에 `document(...)` 호출을 포함한다.
- snippet identifier는 `{모듈}/{행위}` 형식을 쓴다. (예: `user/register`, `dog/register`) 실패 응답은 `{모듈}/{행위}-error`.
- `document`는 `com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document`를 static import한다. 기존 스니펫에 더해 OpenAPI 스펙의 재료인 `resource.json`이 함께 생성된다. 예외: `GlobalExceptionHandlerTest`는 테스트 전용 `/test/**` 경로가 스펙에 섞이지 않도록 `MockMvcRestDocumentation.document`를 유지한다.
- 문서화 필수 항목 — 요청: path parameters / query parameters / request fields(해당하는 것 모두), 응답: response fields, 실패 응답: 대표 에러 케이스 1개 이상(Problem Details 형식), 인증이 필요한 API의 요청 예시: `Authorization: Bearer access-token` 헤더.
- `.principal(...)`은 인증 주체만 주입하고 HTTP 헤더를 만들지 않는다. 문서 스니펫을 생성하는 요청에는 `.principal(...)`과 별도로 위 인증 헤더를 추가한다.

새 API를 추가한 PR에는 다음을 함께 포함한다.

- `src/docs/asciidoc/index.adoc`에 해당 API 섹션 추가 (형식과 Swagger 딥링크 규칙은 docs/api-docs.md)
- 인증 없이 호출 가능한 공개 API면 `build.gradle.kts`의 `publicPaths` 목록 갱신
- 새 모듈이면 `build.gradle.kts`의 `moduleTags` 매핑 갱신

## 커버리지

- JaCoCo 라인 커버리지를 CI에서 검증하며, 70% 미만이면 빌드가 실패한다.
- `MeongcoachApplication`과 `shared/config`는 검증에서 제외한다. `shared/config`에는 설정만 두고 검증 로직은 커버리지 대상 패키지에 둔다.

## 아키텍처 검증

아키텍처 규칙은 문서로만 남기지 않고 ArchUnit 단위 테스트(`archunit-junit5`)로 강제한다. 모듈 경계는 `architecture/ModularityTest`의 `ApplicationModules.verify()`가 검증한다. ([docs/architecture.md](../../../../docs/architecture.md) 참고)

- 전 모듈 공통 아키텍처 테스트는 `src/test/java/com/daesabu/meongcoach/architecture/`에 두고, 테스트 클래스가 곧 검증 범주다 — 계층 의존 방향(`LayerDependencyTest`), 역할별 네이밍(`NamingTest`), 애노테이션 적용 위치와 도메인의 스프링 비의존(`AnnotationPatternTest`), 도메인 입력 모델(`DomainInputModelTest`). 새 아키텍처 규칙은 새 파일을 만들지 않고 해당 범주 테스트에 추가한다.
- `ClassFileImporter`로 임포트한 `JavaClasses` 상수를 두고, 일반 `@Test` 메서드에서 `ArchRule.check()`로 검증한다. 다른 테스트와 같이 `@Test` 메서드명으로 검증 의도를 서술하기 위해 `@ArchTest` 필드 방식은 쓰지 않는다.
- `dependOnClassesThat`은 필드·파라미터·리턴 타입·상속 등 선언 수준까지 포함하는 넓은 검증이라 계층 격리 규칙에 쓰고, `accessClassesThat`은 메서드 호출·필드 접근 등 실행 코드 수준의 좁은 검증이라 특정 API 호출 금지 규칙에 쓴다. 무엇을 막으려는지에 따라 구분한다.
