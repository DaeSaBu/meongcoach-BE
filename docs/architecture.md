# 아키텍처

## Spring Modulith 기반 모듈형 모놀리스

모놀리스로 시작하되, 도메인(기능) 단위의 모듈 경계를 Spring Modulith로 강제합니다.
각 최상위 패키지가 하나의 모듈이며, 모듈은 향후 MSA로 분리할 수 있는 단위입니다.
모듈 내부는 `adapter / application / domain` 계층으로 구성합니다.

## 패키지 구조

```
com.daesabu.meongcoach
├── user                              ← Spring Modulith 모듈 (MSA 분리 단위)
│   ├── package-info.java             // @ApplicationModule 선언
│   ├── adapter
│   │   └── webapi/                   // UserController, dto/
│   ├── application
│   │   ├── provided/                 // UserRegister — named interface (모듈 공개 API)
│   │   ├── required/                 // EmailSender, UserRepository — 모듈이 필요로 하는 외부 자원
│   │   ├── UserQueryService
│   │   └── UserFinderService
│   └── domain
│       ├── User
│       └── UserRegisterRequest
├── walk                              ← 모듈 (동일 구조)
├── matching                          ← 모듈 (동일 구조)
└── shared                            ← 횡단 관심사
    ├── security
    └── integration
```

## 모듈 규칙

- **최상위 패키지 1개 = 모듈 1개 = MSA 분리 단위.** 새 기능 영역은 새 최상위 패키지(모듈)로 추가합니다.
- 각 모듈 루트에 `package-info.java`를 두고 `@ApplicationModule`을 선언합니다.
- **모듈 간 접근은 `application/provided`의 인터페이스로만 합니다.** 다른 모듈의 서비스 구현체, `required` 인터페이스, 도메인 내부에 직접 접근하지 않습니다. (provided 인터페이스 시그니처에 노출된 도메인 타입은 참조 가능)
- `shared`는 security, integration 등 횡단 관심사만 담습니다. 모든 모듈이 `shared`를 참조할 수 있지만, `shared`는 어떤 모듈도 참조하지 않습니다.

## 모듈 내부 계층

| 계층 | 책임 | 내용 |
|---|---|---|
| `adapter` | 외부 세계와의 연결 | `webapi/` — 컨트롤러, 웹 요청/응답 DTO. 웹 기술(Spring MVC)은 여기에만 둔다 |
| `application` | 유스케이스 | `provided/` — 모듈이 외부에 공개하는 인터페이스, `required/` — 모듈이 필요로 하는 자원 인터페이스(리포지토리, 메일 등), 그리고 이를 구현·사용하는 서비스 |
| `domain` | 도메인 모델·로직 | 엔티티(`User`), 도메인 입력 모델(`UserRegisterRequest`) |

### 계층 의존 방향

모듈 내부 의존은 항상 `adapter → application → domain`입니다. 역방향 의존은 금지합니다.

| 계층 | 참조 가능 대상 | 비고 |
|---|---|---|
| `domain` | 없음 | 순수 도메인. JPA 매핑 어노테이션은 허용 |
| `application` | `domain` | `@Service` 허용. `adapter` 참조 금지 |
| `adapter` | `application`(주로 `provided`), `domain` | 웹 등 기술 의존은 여기에만 |

- `required/`의 리포지토리 인터페이스(예: `UserRepository`)는 Spring Data JPA가 런타임에 구현합니다. 별도 영속성 어댑터 클래스를 만들지 않습니다.
- 메일 발송 등 외부 시스템 연동이 필요하면 `required/`에 인터페이스(예: `EmailSender`)를 정의하고, 구현은 `shared/integration` 또는 해당 모듈의 `adapter`에 둡니다.

## 요청 처리 흐름 (user 모듈 예시)

```
HTTP 요청
  → UserController          (adapter/webapi)
  → UserRegister            (application/provided, 인터페이스)
  → UserRegisterService     (application, 구현체)
  → User                    (domain, 비즈니스 로직 수행)
  → UserRepository          (application/required → Spring Data JPA가 구현)
  → DB
```

- 컨트롤러는 `provided` 인터페이스에만 의존하며, 웹 DTO ↔ 도메인 입력 모델 변환까지만 담당합니다.
- 서비스는 도메인 객체를 조합해 유스케이스를 수행하고, 외부 자원은 `required` 인터페이스를 통해서만 접근합니다.

## 모듈 경계 검증

모듈 경계 위반은 테스트로 검증합니다.

```java
@Test
void verifyModularity() {
	ApplicationModules.of(MeongcoachApplication.class).verify();
}
```

> 참고: Spring Modulith 의존성(`spring-modulith-starter-core`, 테스트용 `spring-modulith-starter-test`)이 `build.gradle.kts`에 추가되어야 합니다.

클래스 네이밍 규칙은 [docs/conventions/code-convention.md](conventions/code-convention.md)를 따릅니다.
