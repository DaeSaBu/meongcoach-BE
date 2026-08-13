# 예외 처리 컨벤션

예외는 각 모듈이 자기 도메인에 맞게 정의해서 **던지기만** 하고, HTTP 에러 응답으로의 변환은 전역 핸들러(`shared/webapi/GlobalExceptionHandler`)가 전담합니다. 모든 에러 응답은 RFC 9457(구 RFC 7807) Problem Details 표준을 따릅니다.

- 컨트롤러/서비스에 개별 `@ExceptionHandler`를 만들지 않습니다.
- 예외를 catch해서 에러 DTO를 직접 조립해 반환하지 않습니다.
- 예상치 못한 예외도 전역 핸들러의 fallback이 500 응답으로 변환하므로, 별도 방어 코드가 필요 없습니다.

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

`shared/exception`은 **순수 Java**로 유지합니다. `domain` 계층은 Spring에 의존할 수 없으므로(archunit-convention.md), `ErrorCode.status()`는 `HttpStatus`가 아닌 `int`를 반환하고 `HttpStatus` 변환은 웹 계층인 `GlobalExceptionHandler`에서만 수행합니다. 같은 이유로 `domain`은 `shared/exception`만 참조할 수 있고 `shared/webapi`는 참조하지 않습니다.

## 새 모듈에서 예외 정의하기 (4단계)

살아있는 예시는 `user/domain/exception`을 보세요. 아래는 `UserNotFoundException`을 기준으로 한 절차입니다.

### ① `domain/exception`에 `{모듈}ErrorCode` enum을 정의합니다

모듈당 1개, `ErrorCode`를 구현합니다. `code()`는 `name()`을 반환하므로 enum 상수 이름이 곧 에러 코드입니다. 상수 이름은 `{모듈}_{원인}` 형식의 UPPER_SNAKE_CASE로 지어 전역에서 유일하게 만듭니다. (구현 형태는 `user/domain/exception/UserErrorCode` 참고)

```java
public enum UserErrorCode implements ErrorCode {

	USER_NOT_FOUND(404, "회원을 찾을 수 없습니다."),
	USER_INVALID_REFRESH_TOKEN(401, "리프레시 토큰이 유효하지 않습니다."),
	...
```

### ② `domain/exception`에 케이스별 구체 예외 클래스를 정의합니다

`DomainException`을 상속하고 이름은 `~Exception`으로 끝냅니다. 구체 타입이 있어야 테스트에서 `isInstanceOf`로 검증할 수 있습니다.

```java
public class UserNotFoundException extends DomainException {

	public UserNotFoundException(Long userId) {
		super(UserErrorCode.USER_NOT_FOUND, "id가 " + userId + "인 회원을 찾을 수 없습니다.");
	}
}
```

- 기본 메시지로 충분하면 `super(UserErrorCode.USER_NOT_FOUND)`만 호출합니다.
- 상황 정보를 담고 싶으면 두 번째 인자로 detail을 넘깁니다. **detail은 응답에 그대로 노출되므로 비밀번호 등 민감 정보를 넣지 않습니다.**

### ③ 도메인/애플리케이션 로직에서 던집니다

```java
userRepository.findById(userId)
		.orElseThrow(() -> new UserNotFoundException(userId));
```

- 던지기만 하면 됩니다. `GlobalExceptionHandler`가 `ErrorCode`의 status/code/message로 Problem Details 응답을 만듭니다.
- 예외를 catch해서 삼키거나, 로그만 찍고 다시 던지지 않습니다. (로깅은 전역 핸들러가 일괄 수행 — 중복 로깅 금지)

### ④ 컨트롤러 테스트에 실패 케이스를 문서화합니다

restdocs-convention.md에 따라 대표 에러 케이스 1개 이상을 `document("{모듈}/{행위}-error", ...)`로 문서화합니다. (실존 예시: `AuthControllerTest`의 `user/social-login-error`)

```java
mockMvc.perform(post("/api/users/social/{provider}", "kakao")
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidTokenRequest))
		.andExpect(status().isUnauthorized())
		.andExpect(jsonPath("$.code").value("USER_INVALID_SOCIAL_TOKEN"))
		.andDo(document("user/social-login-error", ...));
```

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
| `AccessDeniedException` | 403 | `FORBIDDEN` | 권한 부족 |
| 그 외 모든 `Exception` (fallback) | 500 | `INTERNAL_SERVER_ERROR` | detail에 내부 정보를 노출하지 않음 |

### 시큐리티 필터 체인 예외

`AuthenticationException`과 `AccessDeniedException`은 `DispatcherServlet` **밖**(시큐리티 필터 체인)에서 던져지므로 `@RestControllerAdvice`가 잡지 못합니다.

`shared/security/SecurityExceptionTranslator`가 `AuthenticationEntryPoint`와 `AccessDeniedHandler`를 구현해 이 예외를 Spring MVC의 `handlerExceptionResolver`로 되돌려 보내고, 그 결과 `GlobalExceptionHandler`가 처리합니다. **Problem Details 형식을 만드는 곳은 여전히 한 군데뿐이며, 필터 체인 응답과 컨트롤러 응답이 완전히 동일합니다.**

### 외부 API 연동 예외

외부 시스템 연동 어댑터(`adapter/client`)는 인프라 예외(`RestClientException` 등)를 **그 자리에서 도메인 예외로 변환**합니다. 이는 "예외를 catch해서 에러 DTO를 조립하지 않는다"는 규칙과 충돌하지 않습니다 — DTO를 만드는 것이 아니라 계층 경계에서 예외 타입을 번역하는 것입니다.

호출 실패는 원인에 따라 구분합니다. 예를 들어 소셜 로그인은 "토큰이 무효"(401)와 "제공자와 통신 실패"(502)를 다른 에러 코드로 내려, 클라이언트가 재로그인과 재시도를 구분할 수 있게 합니다.

프레임워크 예외는 `ResponseEntityExceptionHandler` 상속으로 처리되므로 위 표에 없는 Spring MVC 예외도 자동으로 Problem Details 형식이 됩니다.

## 로깅 정책

로깅은 `GlobalExceptionHandler`에서만 수행합니다.

- **4xx**: `warn` 레벨, 스택트레이스 없이 code/message만 기록
- **5xx / fallback**: `error` 레벨, 스택트레이스 포함
- 도메인 코드에서 예외를 로그로 찍고 다시 던지지 않습니다. (핸들러와 중복)

## 테스트 작성법

- 도메인 예외 발생 로직은 순수 단위 테스트로 검증합니다.

```java
assertThatThrownBy(() -> userProfileRegister.register(unknownUserId, command))
		.isInstanceOf(UserNotFoundException.class);
```

- API 실패 응답은 `@WebMvcTest`에서 상태 코드와 `$.code`를 검증합니다. (위 4단계-④ 참고)
- `GlobalExceptionHandler` 자체의 동작을 바꿀 때는 `shared/webapi/GlobalExceptionHandlerTest`에 케이스를 추가합니다. 이 테스트의 픽스처(`TestErrorCode`, `TestNotFoundException`, `ExceptionTriggerController`)가 살아있는 예시 코드 역할도 합니다.
