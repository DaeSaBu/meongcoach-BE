# 인증·보안

소셜 로그인으로 회원을 식별하고, 이후 모든 API 인가는 **자체 발급 JWT**로 합니다.

## 로그인 흐름 — 네이티브 SDK + 서버 검증

클라이언트가 React Native 앱이므로, 서버가 리다이렉트를 주고받는 OAuth2 인가 코드 흐름을 쓰지 않습니다.

```
[앱] 카카오 SDK 네이티브 로그인 (네이티브 앱 키, 앱-투-앱)
  → 카카오 access token 획득
     → POST /api/v1/auth/social/kakao  { "token": "..." }
        → 서버가 카카오 API로 토큰 검증 + 회원 정보 조회
           → 회원 조회·생성 (User + SocialAccount)
              → 우리 JWT 발급
        ← { accessToken, refreshToken, needsOnboarding }
```

**이 방식을 택한 이유**

- 딥링크는 쿠키를 앱에 전달하지 못해, 서버 리다이렉트 방식이었다면 토큰을 URL에 실어야 합니다.
  URL은 브라우저 기록·`Referer` 헤더·서버 액세스 로그에 남고, 이는 TLS가 막아주지 못합니다.
  응답 본문으로 주면 이 문제가 아예 발생하지 않습니다.
- 카카오톡 앱-투-앱 간편로그인을 쓸 수 있어 UX가 낫습니다.
- **서버가 카카오 REST 키·시크릿을 보관하지 않습니다.** 앱이 네이티브 앱 키를 쓰고,
  서버가 아는 카카오 값은 공개 식별자인 앱 ID 하나뿐입니다.
- 나중에 Apple을 붙일 때 ES256 `client_secret` JWT 생성과 `.p8` 키 관리가 필요 없습니다
  (서버가 인가 코드 교환을 하지 않으므로).

카카오가 발급한 토큰은 **로그인 시점에만 쓰고 버립니다.** 이후 인가는 전적으로 우리 JWT로 합니다.
제공자 토큰은 "제공자 API를 호출할 권한"이지 "우리 서비스를 쓸 권한"이 아니고, 우리 권한 정보를 담을 수도 없기 때문입니다.

## app_id 검증 — 생략하면 안 되는 이유

카카오 액세스 토큰은 bearer 토큰이라 "어떤 카카오 앱이 사용자 X의 토큰을 갖고 있다"만 증명하고,
**"우리 앱이 발급받은 토큰"임은 증명하지 않습니다.**

검증이 없으면 공격자가 자신의 카카오 앱에서 받은 유효한 토큰을 우리 서버에 제출해
**해당 카카오 사용자로 로그인할 수 있습니다** (confused deputy).

그래서 `KakaoSocialProfileReader`는 `GET /v1/user/access_token_info`가 돌려주는 `app_id`를
설정된 `meongcoach.social.kakao.app-id`와 대조하고, 다르면 즉시 거부합니다
(`USER_SOCIAL_TOKEN_APP_MISMATCH`). 불일치 시 두 번째 호출은 하지 않습니다.

`/v2/user/me`는 `app_id`를 돌려주지 않으므로 **`access_token_info` 호출은 생략할 수 없습니다.**

Google·Apple을 추가할 때의 대응 검증은 ID 토큰의 `aud` 클레임 확인입니다.

## 토큰 정책

| 항목 | 값 |
|---|---|
| 서명 | HS256 (대칭키) |
| 액세스 토큰 | 1시간 |
| 리프레시 토큰 | 14일 |
| 클레임 | `iss`, `sub`(회원 ID), `iat`, `exp`, `jti`, `token_type` |
| 저장 | **하지 않음.** 서명·만료·용도 검증만 |

### 액세스/리프레시 디코더 분리

`SecurityConfig`는 디코더를 두 개 정의하고 `@Primary`를 두지 않습니다. 각 디코더에
`TokenTypeValidator`가 붙어 `token_type` 클레임을 강제하므로, **리프레시 토큰을 `Authorization: Bearer`로
제출하면 401**입니다. 이 검증이 없으면 리프레시 토큰이 사실상 14일짜리 액세스 토큰이 됩니다.

### 무상태 정책의 트레이드오프

리프레시 토큰을 저장하지 않으므로 **강제 로그아웃·토큰 무효화가 불가능합니다.**
탈취된 리프레시 토큰은 만료(14일)까지 유효합니다.

완화 장치:
- 액세스 토큰 수명을 1시간으로 짧게 유지
- 토큰마다 고유 `jti`를 넣어 두어, 나중에 거부 목록을 붙일 수 있게 함
- 필요 시 `refresh-token-validity` 단축

강제 로그아웃이 요구사항이 되면 리프레시 토큰 영속화가 필요하며, 이는 정책 변경입니다.

## 필터 체인 구성

리다이렉트 흐름이 없어 세션이 필요 없으므로 **무상태 체인 하나**만 둡니다.

- `csrf` / `formLogin` / `httpBasic` / `logout` 비활성화
- `SessionCreationPolicy.STATELESS`
- permitAll: `/api/health`, `/api/v1/auth/**`, h2-console
- 그 외 모든 요청은 인증 필요
- `oauth2ResourceServer.jwt()` — Bearer 토큰 파싱·검증은 프레임워크가 담당하므로 커스텀 필터가 없습니다

### 인증 실패 응답

`AuthenticationException`·`AccessDeniedException`은 `DispatcherServlet` 밖(필터 체인)에서 던져져
`@RestControllerAdvice`가 잡지 못합니다. `SecurityExceptionTranslator`가 이 예외를 Spring MVC의
`handlerExceptionResolver`로 되돌려 보내면 `GlobalExceptionHandler`가 처리하므로,
**Problem Details 형식을 만드는 곳은 여전히 한 군데뿐**입니다.

에러 코드는 `UNAUTHORIZED` / `FORBIDDEN` — "프레임워크 예외는 HTTP 상태 enum 이름" 규칙 그대로입니다.
인증 실패 원인은 공격자에게 힌트가 되므로 `detail`에 일반화된 문구만 담습니다.

> 트레이드오프: 리소스 서버 기본 엔트리 포인트를 교체하므로 `WWW-Authenticate: Bearer` 헤더가 사라집니다.
> 모바일 전용 API라 수용했습니다.

## 제공자 추가 방법

`application/required`의 `SocialProfileReader`가 확장 지점입니다.

```java
public interface SocialProfileReader {
	SocialProvider provider();
	SocialAccountLinkCommand read(String credential);
}
```

`SocialLoginService`가 `List<SocialProfileReader>`를 주입받아 `provider()` 기준 맵으로 만듭니다.
**제공자 추가 = `@Component` 1개 + 설정 블록. 기존 클래스 수정은 없습니다.**

### Google (예정)

Google은 앱에 **ID 토큰(JWT)**을 줍니다. REST 조회가 아니라 JWKS 서명 검증입니다.

- `NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")`
- `JwtIssuerValidator("https://accounts.google.com")` + `aud`가 우리 클라이언트 ID를 포함하는지 검증
- `sub` → `providerId`, `email` → `email`

`spring-security-oauth2-jose`가 이미 클래스패스에 있어 의존성 추가가 없습니다.

### Apple (예정)

Google과 같은 구조(`iss=https://appleid.apple.com`, `jwk-set-uri=https://appleid.apple.com/auth/keys`).
다만 Apple 고유의 함정이 있습니다.

- `email`은 **최초 인증에서만** 내려옵니다. 이후 로그인에서 null로 덮어쓰지 않도록 해야 합니다.
- iOS 번들 ID와 웹 서비스 ID의 `aud`가 달라 후보를 목록으로 받아야 합니다.
- 회원 탈퇴 시 Apple `revoke` 호출이 App Store 심사 요구사항이며, **이때는 ES256 `.p8` 클라이언트 시크릿이 필요합니다.**
  로그인에는 필요 없지만 탈퇴 기능에서 다시 등장합니다.

## 환경 변수

기본값을 두지 않아 미설정 시 **기동에 실패합니다.** 커밋된 개발용 키가 배포 환경으로 흘러가는 것을 막기 위함입니다.

| 변수 | 설명 |
|---|---|
| `JWT_SECRET` | JWT 서명 키. **32바이트 이상** (미달 시 기동 실패) |
| `KAKAO_APP_ID` | 카카오 개발자 콘솔의 앱 ID(숫자). 시크릿이 아닌 식별자 |

로컬은 환경 변수로 export하고, CI/배포는 GitHub Actions secrets로 주입합니다.
테스트는 `src/test/resources/application-test.yml`의 더미 값을 쓰므로 환경 변수가 필요 없습니다.

## 알려진 제약

- **H2 콘솔이 permitAll이고 `frameOptions`가 sameOrigin입니다.** 인메모리 MVP 전용이며,
  실제 배포 전에 반드시 제거하거나 프로파일로 분리해야 합니다.
- **최초 로그인 동시성** — 같은 신규 계정으로 동시에 두 요청이 오면 `(provider, provider_id)`
  유니크 제약 위반으로 한쪽이 500이 될 수 있습니다. 확률이 낮아 MVP에서는 두고, 필요 시
  제약 위반을 잡아 재조회하도록 보완합니다.
- **카카오 이메일은 대체로 null입니다.** `account_email` 동의 항목은 비즈니스 앱 심사가 필요하고,
  사용자가 동의를 거부할 수도 있습니다. `SocialAccount.email`이 nullable이라 동작에는 문제가 없습니다.
- **로그인 가용성이 카카오 가용성에 묶입니다.** `spring.http.clients.read-timeout`(3초)이 최후 방어선이며,
  통신 실패는 `USER_SOCIAL_PROVIDER_UNAVAILABLE`(502)로 토큰 무효(401)와 구분해 응답합니다.
