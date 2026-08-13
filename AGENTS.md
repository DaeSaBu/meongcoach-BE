# 멍코치 Back-End 규칙

멍코치 백엔드 저장소입니다. Spring Boot 4.1 / Java 25 / Gradle(Kotlin DSL) / JPA + H2(테스트)/PostgreSQL(로컬·배포) 기반이며, 팀원 3명이 협업합니다.
대응되는 프론트엔드는 ReactNative 기반의 모바일 앱 서비스입니다.

## 핵심 규칙

- 코드 스타일의 원천은 `.editorconfig`(Wooteco 스타일)입니다. 도구로 강제되지 않는 규칙(삼항 연산자·else·switch 금지, return 인자에 호출 중첩 금지)은 code-convention.md 참고.
- 커밋 메시지는 `type(scope): 한글 설명` 형식이며 scope는 선택입니다. (예: `feature(user): 소셜 로그인 API 추가` — 허용 타입·scope 후보는 git-convention.md 참고, `feat`이 아니라 `feature`)
- 아키텍처는 Spring Modulith 기반 모듈 구조를 따릅니다. 모듈 내부 의존 방향은 항상 `adapter → application → domain`이며, 모듈 간 접근은 `application/provided` 인터페이스로만 합니다.
- 로그인 사용자 식별은 컨트롤러의 `@CurrentUserId Long userId` 파라미터로 받습니다. 인증 주체를 회원 ID로 해석하는 곳은 `shared/security/CurrentUserIdArgumentResolver` 한 곳뿐이며, 컨트롤러는 인증 주체를 직접 다루지 않습니다. (architecture.md 참고)
- 애플리케이션 조회 결과는 도메인 타입 반환이 기본입니다. `~View` record는 도메인 타입으로 부족할 때만 만듭니다 — 판단 기준은 code-convention.md 참고.
- 트랜잭션은 서비스 구현 클래스에 `@Transactional(readOnly = true)`를 붙여 기본값을 읽기 전용으로 두고, 쓰기 메서드에만 `@Transactional`로 오버라이드합니다. 메서드 어노테이션은 클래스 설정을 병합하지 않고 통째로 대체합니다. (code-convention.md 참고)
- `main` 브랜치에 직접 push하지 않습니다. 모든 변경은 작업 브랜치에서 PR을 통해 merge합니다.
- 문서, 커밋 메시지, 코드 주석은 한국어로 작성합니다.
- 예외는 각 모듈 `domain/exception`에 `{모듈}ErrorCode` enum + `DomainException` 하위 클래스로 정의해 던지기만 하고, 에러 응답 변환은 전역 핸들러가 RFC 9457 Problem Details 형식으로 전담합니다. 컨트롤러/서비스에서 개별 처리하지 않습니다.
- 인증은 소셜 제공자 토큰을 서버가 검증한 뒤 자체 JWT를 발급하는 무상태 방식입니다. 리프레시 토큰은 저장하지 않으므로 강제 로그아웃·토큰 무효화 기능을 전제로 한 코드를 만들지 않습니다. (security.md 참고)
- API 경로는 `/api/{리소스}` 형태로 쓰고 버전을 붙이지 않습니다. (예: `/api/users`, `/api/dogs`, 헬스 체크는 `/api/health`)

## 상세 문서

| 문서                                                                         | 내용                         |
|----------------------------------------------------------------------------|----------------------------|
| [docs/conventions/git-convention.md](docs/conventions/git-convention.md)   | 커밋 메시지, 브랜치 전략, PR 규칙      |
| [docs/conventions/code-convention.md](docs/conventions/code-convention.md) | 코드 스타일, 네이밍, Lombok/DTO 규칙 |
| [docs/conventions/test-convention.md](docs/conventions/test-convention.md) | 테스트 작성 규칙                  |
| [docs/conventions/restdocs-convention.md](docs/conventions/restdocs-convention.md) | RestDocs API 문서화 규칙   |
| [docs/conventions/archunit-convention.md](docs/conventions/archunit-convention.md) | ArchUnit 아키텍처 검증 규칙  |
| [docs/conventions/exception-convention.md](docs/conventions/exception-convention.md) | 예외 정의 패턴, Problem Details 에러 응답 규칙 |
| [docs/architecture.md](docs/architecture.md)                               | Spring Modulith 모듈 구조, 의존 규칙   |
| [docs/security.md](docs/security.md)                                       | 인증 흐름, JWT 토큰 정책, 소셜 제공자 확장 방법 |
| [docs/ci-cd.md](docs/ci-cd.md)                                             | CI/CD 파이프라인, 커버리지·배포 기준 |
| [docs/profiles.md](docs/profiles.md)                                       | local/dev/prod 프로파일 구성, DB·ddl-auto 정책 |

## 문서 추가 방법

새 컨벤션이나 결정 사항이 생기면 다음 절차로 추가합니다.

1. `docs/` 아래에 마크다운 파일을 만듭니다. 컨벤션은 `docs/conventions/`, 그 외(아키텍처, 운영 등)는 `docs/` 바로 아래에 둡니다.
2. 위 [상세 문서](#상세-문서) 표에 링크를 등록합니다.
3. 모든 팀원과 에이전트에게 항상 적용되어야 하는 규칙이라면 [핵심 규칙](#핵심-규칙)에도 한 줄로 요약해 추가합니다.
