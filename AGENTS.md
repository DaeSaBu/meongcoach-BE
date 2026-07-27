# 멍코치 Back-End 규칙

멍코치 백엔드 저장소입니다. Spring Boot 4.1 / Java 25 / Gradle(Kotlin DSL) / JPA + H2 기반이며, 팀원 3명이 협업합니다.

## 핵심 규칙

- 코드 스타일은 `.editorconfig`(Wooteco 스타일)를 따릅니다 — 탭 들여쓰기, 한 줄 120자 제한, 중괄호 항상 사용.
- 커밋 메시지는 `type: 한글 설명` 형식을 사용합니다. (예: `feature(user): 회원 가입 API 추가` — 허용 타입은 git-convention.md 참고, `feat`이 아니라 `feature`)
- 아키텍처는 Spring Modulith 기반 모듈 구조를 따릅니다. 모듈 내부 의존 방향은 항상 `adapter → application → domain`이며, 모듈 간 접근은 `application/provided` 인터페이스로만 합니다.
- 로그인 사용자 식별은 Spring Security 도입 전까지 `X-User-Id` 헤더 + `@LoginUser` 파라미터로 임시 처리합니다. 교체 지점은 `shared/webapi/LoginUserArgumentResolver` 한 곳입니다. (architecture.md 참고)
- `main` 브랜치에 직접 push하지 않습니다. 모든 변경은 작업 브랜치에서 PR을 통해 merge합니다.
- 문서, 커밋 메시지, 코드 주석은 한국어로 작성합니다.
- 예외는 각 모듈 `domain`에 `{모듈}ErrorCode` enum + `DomainException` 하위 클래스로 정의해 던지기만 하고, 에러 응답 변환은 전역 핸들러가 RFC 9457 Problem Details 형식으로 전담합니다. 컨트롤러/서비스에서 개별 처리하지 않습니다.

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
| [docs/ci.md](docs/ci.md)                                                   | CI 파이프라인, 커버리지 기준        |

## 문서 추가 방법

새 컨벤션이나 결정 사항이 생기면 다음 절차로 추가합니다.

1. `docs/` 아래에 마크다운 파일을 만듭니다. 컨벤션은 `docs/conventions/`, 그 외(아키텍처, 운영 등)는 `docs/` 바로 아래에 둡니다.
2. 위 [상세 문서](#상세-문서) 표에 링크를 등록합니다.
3. 모든 팀원과 에이전트에게 항상 적용되어야 하는 규칙이라면 [핵심 규칙](#핵심-규칙)에도 한 줄로 요약해 추가합니다.
