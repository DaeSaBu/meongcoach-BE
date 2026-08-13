# 코드 컨벤션

## 코드 스타일

스타일의 원천은 저장소 루트의 `.editorconfig`와 `.idea/codeStyles/`입니다. (Wooteco 코드 스타일 기반)
이 문서에 스타일 규칙을 중복 기술하지 않으며, IDE가 자동 적용하는 설정을 그대로 따릅니다.

도구로 강제되지 않아 리뷰에서 확인하는 규칙만 아래에 둡니다.

- 삼항 연산자(`조건 ? A : B`) 금지 — `Objects.requireNonNullElse`, if + early return, 의도가 드러나는 메서드 추출로 대체합니다.
- `else`(`else if` 포함) 금지 — guard clause와 early return으로 분기를 평탄화합니다.
- `switch` 금지 — if + early return, Map 조회, 또는 enum 메서드/다형성으로 대체합니다.
- return 문 **인자에 호출 중첩 금지** — 결과 객체를 `return new ~(...)`로 바로 반환하는 것은 허용하되, 그 인자 자리에 서비스 호출 등 다른 메서드 호출을 넣지 않습니다. 필요한 값은 지역 변수로 먼저 준비해 전달합니다. 단, DTO·View 정적 팩토리(`from`, `of`)의 단순 필드 매핑은 허용합니다.

## 네이밍

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| 패키지(모듈) | 소문자, 단수형 | `user`, `dog`, `shared` |
| 클래스 | PascalCase | `SocialLoginService` |
| 메서드/변수 | camelCase | `findByName` |
| 상수 | UPPER_SNAKE_CASE | `MAX_WALK_TIME` |

### 아키텍처 역할별 네이밍

| 역할 | 위치 | 규칙 | 예시 |
| --- | --- | --- | --- |
| 모듈 공개 API 인터페이스 | `application/provided` | 능력을 나타내는 이름, `~Service` 접미사 없음 | `DogRegister`, `MbtiFinder` |
| 애플리케이션 조회 결과 래퍼 | `application/provided` | `~View` (record) — 도메인 타입만으로 부족할 때만 | `CurriculumListView`, `LessonView` |
| 필요 자원 인터페이스 | `application/required` | 자원 이름 그대로 | `UserRepository`, `VideoStorage` |
| 애플리케이션 서비스 | `application` | `~Service` | `SocialLoginService`, `CurriculumQueryService` |
| 컨트롤러 | `adapter/webapi` | `~Controller` | `AuthController` |
| 외부 API 연동 포트 | `application/required` | 자원 이름 그대로 | `SocialProfileReader` |
| 외부 API 연동 구현 | `adapter/client` | `{제공자}~` | `KakaoSocialProfileReader` |
| 도메인 모델 | `domain` | 개념 이름 그대로 | `User` |
| 도메인 입력 모델 | `domain` | `~Command` (record) | `DogRegisterCommand` |
| 값 객체 | `domain/vo` | 개념 이름 그대로 | `Email` |
| 도메인 예외·에러코드 | `domain/exception` | `{모듈}ErrorCode`, `~Exception` | `UserErrorCode`, `InvalidEmailException` |

- `domain` 루트에는 엔티티와 enum을 두고, 값 객체는 `domain/vo`, 예외·에러코드는 `domain/exception`으로 분리합니다.

## 트랜잭션

- `@Transactional`은 `application` 계층의 서비스 **구현 클래스**에만 붙입니다. `application/provided` 인터페이스에는 붙이지 않습니다. (트랜잭션 경계는 계약이 아니라 구현 관심사)
- 서비스 클래스에 `@Transactional(readOnly = true)`를 붙여 기본값을 읽기 전용으로 두고, **쓰기 메서드에만** `@Transactional`로 오버라이드합니다. 쓰기 메서드만 있는 서비스도 동일하게 적용합니다.
	- 어노테이션 순서는 `@Service` → `@RequiredArgsConstructor` → `@Transactional(readOnly = true)`.
- 메서드 어노테이션은 클래스 설정과 **병합되지 않고 통째로 대체**합니다. `@Transactional(timeout = 5)`처럼 일부 속성만 쓰면 `readOnly`가 기본값 `false`로 리셋되므로, 조회 메서드에 다른 속성이 필요하면 `readOnly = true`를 함께 명시합니다.
- 같은 클래스 내부 호출(`this.method()`)은 프록시를 거치지 않아 트랜잭션이 걸리지 않습니다. 별도 빈으로 분리해 해결하고, `AopContext.currentProxy()`나 자기 주입은 쓰지 않습니다.

## Lombok 사용 규칙

- 허용: `@Getter`, `@RequiredArgsConstructor`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`(JPA 엔티티), `@Slf4j`(로깅)
- 지양: `@Setter`, `@Data`, `@AllArgsConstructor`, `@Builder`, `@ToString`(엔티티 — 연관관계 순환 참조 위험)
- 도메인 객체의 상태 변경은 setter가 아닌 의도가 드러나는 메서드로 표현합니다.

## DTO

- 요청/응답 DTO는 Java `record`로 작성합니다.
- 웹 요청/응답 DTO는 `adapter/webapi/dto`에 두고, 접미사는 요청 `~Request`, 응답 `~Response`를 사용합니다. (예: `SocialLoginRequest`, `SocialLoginResponse`)
- 외부 API 응답 DTO는 `adapter/client/dto`에 `~Response` record로 두고, 필드 매핑은 `@JsonProperty`로 지정합니다. 전역 네이밍 전략(`spring.jackson.property-naming-strategy`)을 바꾸면 우리 API 응답까지 영향을 받으므로 쓰지 않습니다.
- 도메인 입력 모델은 `~Command` 접미사의 record로 `domain`에 두며, 웹 DTO와 별개로 유지합니다. (예: `DogRegisterCommand`)
	- 엔티티 정적 팩토리의 순수 값 파라미터가 3개 이상이면 Command로 묶고, 팩토리는 Command를 받아 생성자에 전달합니다.
	- 연관 엔티티는 Command에 담지 않고 별도 인자로 전달합니다. (예: `Curriculum.create(Topic topic, CurriculumCreateCommand command)`)
- 애플리케이션 조회 결과는 **도메인 타입(엔티티·값 객체)을 그대로 반환하는 것이 기본**입니다. 값을 그대로 옮겨 담기만 하는 `~View`는 만들지 않습니다.
	- 다음 중 하나에 해당할 때만 `application/provided`에 `~View` record를 두고 감쌉니다.
		- **도메인 타입 하나로 표현할 수 없을 때** — 여러 애그리거트 조합, 일부 필드만 내리는 projection, 집계값
		- **모듈 경계를 넘는 반환일 때** — `@NamedInterface("provided")`는 provided 패키지에 물리적으로 존재하는 타입만 노출로 인정합니다. 인터페이스 시그니처에 등장하는 도메인 타입은 전파되지 않으므로, 도메인 타입을 반환하면 호출하는 모듈이 `ApplicationModules.verify()`에서 실패합니다
		- **엔티티에 지연 로딩 연관이 있을 때** — `open-in-view: false`(`application.yml`)라 서비스 트랜잭션이 끝나면 준영속이 됩니다. `adapter`에서 연관을 건드리면 `LazyInitializationException`이 납니다
	- `~View`를 둘 때 필드명은 도메인 기준(`id`, `title`, `sortOrder`)으로 두고, 웹 노출 이름(`topicId`, `topicTitle`)으로 바꾸는 일은 `~Response.from(...)` 정적 팩토리에서만 합니다. `~View`에 웹 네이밍을 쓰면 `application`이 `adapter`의 관심사를 떠안게 됩니다.
- DTO ↔ 도메인 변환은 DTO의 정적 팩토리 메서드(`from`, `of`) 또는 `toXxx` 메서드로 처리합니다.
- 엔티티를 컨트롤러 **응답 본문**으로 직접 노출하지 않습니다. `application`이 엔티티를 반환하더라도 컨트롤러는 `~Response.from(엔티티)`로 감싸 내려보냅니다.
