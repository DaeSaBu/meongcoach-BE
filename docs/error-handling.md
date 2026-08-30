# 에러 응답과 전역 예외 처리

모든 에러 응답은 RFC 9457(구 RFC 7807) Problem Details 표준을 따르며, 변환은 `shared/webapi/GlobalExceptionHandler` 한 곳이 전담합니다. 모듈에서 예외를 정의하고 던지는 규칙은 code-convention 스킬([SKILL.md](../.claude/skills/code-convention/SKILL.md) "예외" 절)을 따릅니다. 이 문서는 응답이 어떤 형식이고 핸들러가 무엇을 어떻게 처리하는지를 설명합니다.

## 에러 응답 형식

Content-Type은 항상 `application/problem+json`입니다.

```json
{
	"title": "Not Found",
	"status": 404,
	"detail": "id가 1인 회원을 찾을 수 없습니다.",
	"instance": "/api/users/1",
	"code": "USER_NOT_FOUND",
	"timestamp": "2026-07-23T05:17:25.762615Z"
}
```

| 필드 | 설명 |
|------|------|
| `type` | 에러 유형 URI. 현재는 `about:blank`이라 표준에 따라 생략됨 (에러 문서 페이지가 생기면 URI로 승격) |
| `title` | HTTP 상태 이름 (예: `Not Found`) |
| `status` | HTTP 상태 코드 |
| `detail` | 사람이 읽을 수 있는 에러 설명 (한국어) |
| `instance` | 에러가 발생한 요청 경로 |
| `code` (확장) | 클라이언트 분기용 에러 코드. 도메인 예외는 `{모듈}_{원인}`, 프레임워크 예외는 HTTP 상태 enum 이름 |
| `timestamp` (확장) | 에러 발생 시각 (UTC) |
| `errors` (확장) | 입력값 검증(`@Valid`) 실패 시에만 포함. `[{field, message}]` 목록 |

## 공통 추상화 (shared)

| 위치 | 구성 요소 | 역할 |
|------|----------|------|
| `shared/exception` | `ErrorCode` 인터페이스 | 모듈별 에러 코드 enum이 구현하는 계약 (`code()`, `message()`, `status()`) |
| `shared/exception` | `DomainException` 추상 클래스 | 모든 도메인 예외의 최상위 타입 |
| `shared/webapi` | `GlobalExceptionHandler` | 모든 예외 → Problem Details 변환 + 로깅 |

## 전역 핸들러 처리 범위

| 예외 | 상태 | code | 비고 |
|------|------|------|------|
| `DomainException` 하위 타입 | `ErrorCode.status()` | `ErrorCode.code()` | 도메인 예외 |
| `MethodArgumentNotValidException` | 400 | `BAD_REQUEST` | `@Valid` 실패. `errors` 필드 포함 |
| `HttpMessageNotReadableException` | 400 | `BAD_REQUEST` | JSON 파싱 실패 |
| `NoResourceFoundException` | 404 | `NOT_FOUND` | 매핑 없는 경로 |
| `HttpRequestMethodNotSupportedException` | 405 | `METHOD_NOT_ALLOWED` | 미지원 HTTP 메서드 |
| `HttpMediaTypeNotSupportedException` | 415 | `UNSUPPORTED_MEDIA_TYPE` | 미지원 Content-Type |
| `AuthenticationException` | 401 | `UNAUTHORIZED` | 인증 실패. 원인을 detail에 노출하지 않음 |
| `AccessDeniedException` | 403 | `FORBIDDEN` 또는 `ONBOARDING_NOT_COMPLETED` | 권한 부족. 요청 주체가 `ROLE_ONBOARDING_MEMBER`면 온보딩 화면 분기용 코드로 구분 |
| 그 외 모든 `Exception` (fallback) | 500 | `INTERNAL_SERVER_ERROR` | detail에 내부 정보를 노출하지 않음 |

프레임워크 예외는 `ResponseEntityExceptionHandler` 상속으로 처리되므로 위 표에 없는 Spring MVC 예외도 자동으로 Problem Details 형식이 됩니다.

### 시큐리티 필터 체인 예외

`AuthenticationException`과 `AccessDeniedException`은 `DispatcherServlet` **밖**(시큐리티 필터 체인)에서 던져지므로 `@RestControllerAdvice`가 잡지 못합니다.

`shared/security/SecurityExceptionTranslator`가 `AuthenticationEntryPoint`와 `AccessDeniedHandler`를 구현해 이 예외를 Spring MVC의 `handlerExceptionResolver`로 되돌려 보내고, 그 결과 `GlobalExceptionHandler`가 처리합니다. **Problem Details 형식을 만드는 곳은 여전히 한 군데뿐이며, 필터 체인 응답과 컨트롤러 응답이 완전히 동일합니다.**

## 로깅 정책

로깅은 `GlobalExceptionHandler`에서만 수행합니다.

- **4xx**: `warn` 레벨, 스택트레이스 없이 code/message만 기록
- **5xx / fallback**: `error` 레벨, 스택트레이스 포함

## Sentry 전송

`error` 레벨 로그는 Sentry logback 연동이 이벤트로 보냅니다. 위 로깅 정책에 따라 5xx와 fallback만 전송되고 4xx는 전송되지 않으며, SQS 리스너처럼 컨트롤러 밖에서 남긴 `error` 로그도 같은 기준으로 전송됩니다. `info` 이상 로그는 breadcrumb으로 붙습니다.

- Sentry의 예외 해석기는 가장 뒤 순서(`sentry.exception-resolver-order`)로 두어 전역 핸들러가 처리한 예외를 다시 보고하지 않습니다.
- `SENTRY_DSN`이 없으면 SDK가 꺼집니다. local·test는 주입하지 않고, dev·prod는 CD가 환경별 프로젝트(`meongcoach-{env}-be`)의 DSN과 배포 커밋 SHA(`SENTRY_RELEASE`)를 주입합니다. 환경 태그는 프로파일 파일의 `sentry.environment`가 정합니다.
- `send-default-pii`는 끄므로 요청 IP·사용자 정보는 보내지 않습니다.

## 핸들러 변경 시

`GlobalExceptionHandler` 자체의 동작을 바꿀 때는 `shared/webapi/GlobalExceptionHandlerTest`에 케이스를 추가합니다. 이 테스트의 픽스처(`TestErrorCode`, `TestNotFoundException`, `ExceptionTriggerController`)가 살아있는 예시 코드 역할도 합니다.
