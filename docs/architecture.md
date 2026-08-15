# 아키텍처

## Spring Modulith 기반 모듈형 모놀리스

모놀리스로 시작하되, 도메인(기능) 단위의 모듈 경계를 Spring Modulith로 강제합니다.
각 최상위 패키지가 하나의 모듈이며, 모듈은 향후 MSA로 분리할 수 있는 단위입니다.
모듈 내부는 `adapter / application / domain` 계층으로 구성합니다.

## 패키지 구조

```
com.daesabu.meongcoach
├── user         ← 회원 계정·소셜 로그인·프로필
├── dog          ← 반려견 프로필
├── training     ← 훈련 콘텐츠 카탈로그
├── progress     ← 학습 진도
├── ai           ← AI 영상 분석 리포트 (SQS 컨슈머, EvoLink 연동)
├── media        ← 이미지·영상 업로드 URL 발급 (R2/S3)
├── onboarding   ← 온보딩 흐름 조합
├── health       ← 서비스 상태 확인
└── shared       ← 횡단 관심사 (config / security / webapi / exception / domain)
```

각 모듈의 공통 골격은 다음과 같습니다. 클래스 단위의 실제 구성은 코드가 원천이므로 여기 나열하지 않습니다 — 각 모듈의 `package-info.java`와 `application/provided` 패키지를 보세요.

```
{모듈}
├── package-info.java        // @ApplicationModule 선언
├── adapter                  // 외부 연결 — webapi/ integration/ security/ consumer/
├── application
│   ├── provided/            // 모듈 공개 API (@NamedInterface("provided"))
│   ├── required/            // 모듈이 필요로 하는 자원 인터페이스
│   └── ~Service             // 유스케이스 구현
└── domain                   // 엔티티, vo/, exception/, 입력 모델(~Command)
```

모듈은 필요한 계층만 갖습니다. `dog`·`progress`는 자체 API 없이 `provided` 인터페이스로만 노출되어 `adapter`가 없고, `onboarding`은 다른 모듈을 조합만 하므로 `domain`이 없습니다. 문서나 코드를 생성할 때 없는 계층을 만들어 채우지 않습니다.

## 모듈 규칙

- **최상위 패키지 1개 = 모듈 1개 = MSA 분리 단위.** 새 기능 영역은 새 최상위 패키지(모듈)로 추가합니다.
- 각 모듈 루트에 `package-info.java`를 두고 `@ApplicationModule`을 선언합니다.
- **모듈 간 접근은 `application/provided`의 인터페이스로만 합니다.** 다른 모듈의 서비스 구현체, `required` 인터페이스, 도메인 내부에 직접 접근하지 않습니다. `verify()`는 provided 패키지에 **물리적으로 존재하는 타입만** 노출로 인정하며, 인터페이스 시그니처에 등장하는 도메인 타입은 전파되지 않습니다. 따라서 모듈 경계를 넘나드는 값은 provided 패키지의 record로 정의해야 합니다. (선례: `DogRegisterInfo`, `TopicSummary`, `VideoUploadUrlResult`)
- **`application/provided`에는 `package-info.java`로 `@NamedInterface("provided")`를 선언합니다.** 선언하지 않으면 Modulith가 이 패키지를 모듈 내부로 취급해 다른 모듈에서의 호출이 `verify()`에서 실패합니다.
- `shared`는 보안·설정 등 횡단 관심사만 담습니다. 모든 모듈이 `shared`를 참조할 수 있지만, `shared`는 어떤 모듈도 참조하지 않습니다.

## 모듈 내부 계층

| 계층 | 책임 | 내용 |
|---|---|---|
| `adapter` | 외부 세계와의 연결 | `webapi/` — 컨트롤러, 웹 요청/응답 DTO. `integration/` — 외부 API 호출 구현과 응답 DTO. `security/` — Spring Security 연동 지점. `consumer/` — 메시지 큐 컨슈머(예: `ai/adapter/consumer/VideoUploadSqsConsumer`). 기술 의존은 여기에만 둔다 |
| `application` | 유스케이스 | `provided/` — 모듈이 외부에 공개하는 인터페이스, `required/` — 모듈이 필요로 하는 자원 인터페이스(리포지토리, 메일 등), 그리고 이를 구현·사용하는 서비스 |
| `domain` | 도메인 모델·로직 | 엔티티(`User`), 값 객체(`vo/`), 예외(`exception/`), 도메인 입력 모델(`~Command`) |

### 계층 의존 방향

모듈 내부 의존은 항상 `adapter → application → domain`입니다. 역방향 의존은 금지합니다.

| 계층 | 참조 가능 대상 | 비고 |
|---|---|---|
| `domain` | 없음 | 순수 도메인. JPA 매핑 어노테이션은 허용 |
| `application` | `domain` | `@Service` 허용. `adapter` 참조 금지 |
| `adapter` | `application`(주로 `provided`), `domain` | 웹 등 기술 의존은 여기에만 |

- `required/`의 리포지토리 인터페이스(예: `UserRepository`)는 Spring Data JPA가 런타임에 구현합니다. 별도 영속성 어댑터 클래스를 만들지 않습니다.
- 외부 시스템 연동이 필요하면 `required/`에 인터페이스(예: `SocialProfileReader`)를 정의하고, **구현은 해당 모듈의 `adapter/integration`에 둡니다.** `shared`에 두면 `shared`가 모듈의 인터페이스를 참조하게 되어 순환이 생기고 `ApplicationModules.verify()`가 실패합니다.
- `shared`가 모듈의 구현체를 써야 한다면 **프레임워크 인터페이스 타입으로만** 주입받습니다. (예: `AuthenticationEntryPoint`)

## 요청 처리 흐름 (user 모듈 예시)

```
HTTP 요청
  → AuthController          (adapter/webapi)
  → SocialLogin             (application/provided, 인터페이스)
  → SocialLoginService      (application, 구현체)
  → User, SocialAccount     (domain, 비즈니스 로직 수행)
  → UserRepository          (application/required → Spring Data JPA가 구현)
  → DB
```

- 컨트롤러는 `provided` 인터페이스에만 의존하며, 웹 DTO ↔ 도메인 입력 모델 변환까지만 담당합니다.
- 서비스는 도메인 객체를 조합해 유스케이스를 수행하고, 외부 자원은 `required` 인터페이스를 통해서만 접근합니다.

## 로그인 사용자 식별 (@CurrentUserId)

컨트롤러는 `@CurrentUserId Long userId` 파라미터로 사용자를 받을 뿐이고, 인증 주체를 회원 ID로 해석하는 일은 `shared/security/CurrentUserIdArgumentResolver`가 전담합니다(`shared/config/WebConfig`에서 등록). 리졸버는 요청의 `Principal`(시큐리티 필터 체인이 세운 `JwtAuthenticationToken`, 이름이 곧 액세스 토큰의 `sub` 클레임)을 꺼내 `Long`으로 변환합니다. 토큰의 유효성(서명·만료·발급자·용도) 검증은 시큐리티 필터 체인이 이미 끝냈으므로 리졸버는 다시 검증하지 않습니다. ([security.md](security.md) 참고)

인증 정보가 없거나 주체 이름이 회원 ID 형식이 아니면 `AuthenticationException` 계열 예외를 던지고, 전역 핸들러가 401 Problem Details로 변환합니다. 다만 보호된 경로는 필터 체인이 먼저 401로 막으므로, 이 분기는 방어적 장치입니다.

리졸버가 보는 것은 서블릿 표준 `Principal`뿐이라 `SecurityContextHolder`·`Jwt` 같은 인증 구현 타입에 묶이지 않습니다. 인증 방식이 바뀌어도 필터 체인이 주체를 세우기만 하면 리졸버와 `@CurrentUserId`를 쓰는 컨트롤러 시그니처는 그대로 둘 수 있습니다.

테스트에서는 컨트롤러 슬라이스에 필터 체인이 없어(`test-convention.md`) 요청 빌더의 `.principal(...)`로 인증 주체를 직접 세웁니다. 필터 체인이 세운 주체를 리졸버가 실제로 읽어내는지는 `shared/config/SecurityFilterChainTest`가 확인합니다.

## 모듈 경계 검증

모듈 경계 위반은 테스트로 검증합니다.

`architecture/ModularityTest`가 `ApplicationModules.verify()`로 모듈 경계 위반을 검증합니다.

클래스 네이밍 규칙은 [docs/conventions/code-convention.md](conventions/code-convention.md)를 따릅니다.
