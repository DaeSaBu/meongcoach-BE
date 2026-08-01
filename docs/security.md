# 인증·보안

소셜 로그인으로 회원을 식별하고, 이후 모든 API 인가는 **자체 발급 JWT**로 합니다.

## 로그인 흐름 — 네이티브 SDK + 서버 검증

클라이언트가 React Native 앱이므로, 서버가 리다이렉트를 주고받는 OAuth2 인가 코드 흐름을 쓰지 않습니다.

```
[앱] 카카오 SDK 네이티브 로그인 (네이티브 앱 키, 앱-투-앱, OIDC)
  → 카카오 id_token 획득
     → POST /api/users/social/kakao  { "token": "..." }
        → 서버가 id_token 서명·발급자·만료·aud 검증 (캐시된 공개 키로 로컬 검증)
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
  서버가 아는 카카오 값은 공개 식별자인 네이티브 앱 키(`aud`)뿐입니다.
- 나중에 Apple을 붙일 때 ES256 `client_secret` JWT 생성과 `.p8` 키 관리가 필요 없습니다
  (서버가 인가 코드 교환을 하지 않으므로).

**BFF 패턴을 쓰지 않는 이유**

BFF 토큰 핸들링 패턴이 사는 이익은 "토큰을 JS가 못 읽는 httpOnly 쿠키에 두어 XSS를 막는다" 하나입니다.
React Native는 DOM이 없어 XSS 공격면이 없고 Keychain/Keystore라는 OS 보안 저장소가 있으므로,
**비용만 내고 이익은 얻지 못합니다.** 또 로그인 리다이렉트가 일어나는 `ASWebAuthenticationSession`/
Custom Tabs의 쿠키 저장소는 RN `fetch`의 네이티브 쿠키 저장소와 분리되어 있어, 세션 쿠키를 내려도
앱의 API 호출에 실리지 않습니다. 결국 딥링크로 일회용 코드를 넘겨 재교환하는 핸드오프를 다시 만들어야 합니다.

전환을 재검토할 조건은 셋입니다.

1. 서버가 사용자 대신 제공자 API를 호출해야 함 (예: 카카오톡으로 리포트 발송) — 제공자 리프레시 토큰 보관 필요
2. 웹 클라이언트를 함께 서비스 — 이때는 BFF의 XSS 방어가 실제 이익이 됨
3. 네이티브 SDK 유지보수 부담이 감당 불가 수준이 됨

리프레시 토큰 저장·강제 로그아웃은 이 목록에 없습니다. 무효화 대상은 **우리가 발급한** 리프레시 토큰이고,
저장 여부는 로그인 방식과 무관하게 언제든 바꿀 수 있습니다 (아래 "무상태 정책의 트레이드오프" 참고).

카카오가 발급한 토큰은 **로그인 시점에만 쓰고 버립니다.** 이후 인가는 전적으로 우리 JWT로 합니다.
제공자 토큰은 "제공자 API를 호출할 권한"이지 "우리 서비스를 쓸 권한"이 아니고, 우리 권한 정보를 담을 수도 없기 때문입니다.

local·dev 프로파일에서는 `POST /api/users/social/kakao`에 `DEV_LOGIN_TOKEN`을 보내 고정 개발 계정의 JWT를
발급받습니다. 두 프로파일에서는 실제 카카오 리더 대신 이 리더가 등록되며 test·prod에는 존재하지 않습니다.

## aud 검증 — 생략하면 안 되는 이유

id_token의 서명이 유효하다는 것은 "카카오가 발급했다"만 증명하고,
**"우리 앱에 발급했다"는 증명하지 않습니다.**

검증이 없으면 공격자가 자신의 카카오 앱에서 받은 유효한 토큰을 우리 서버에 제출해
**해당 카카오 사용자로 로그인할 수 있습니다** (confused deputy).

그래서 `KakaoSocialProfileReader`는 id_token의 `aud`가 설정된
`meongcoach.social.kakao.audiences`에 포함되는지 대조하고, 아니면 거부합니다
(`USER_SOCIAL_TOKEN_APP_MISMATCH`). `aud`가 아예 없는 토큰도 같은 이유로 거부합니다.

`aud`는 플랫폼마다 다릅니다 — **네이티브 앱은 네이티브 앱 키, 웹은 REST API 키**입니다.
그래서 단일 값이 아니라 목록으로 받습니다. 여기에는 반드시 우리 앱의 키만 넣어야 합니다.

`aud` 검증만 디코더의 `OAuth2TokenValidator` 체인이 아니라 `read()`에서 직접 합니다.
검증기에 넣으면 서명 실패와 같은 `JwtValidationException`으로 뭉개져
`USER_INVALID_SOCIAL_TOKEN`과 구분할 수 없기 때문입니다. 이 검증은 별도 에러 코드를 유지할 값어치가 있습니다.

Google·Apple도 동일하게 ID 토큰의 `aud` 클레임 확인입니다.

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

리다이렉트 흐름이 없어 세션이 필요 없으므로 **무상태 체인 하나**를 기본으로 둡니다.

- `csrf` / `formLogin` / `httpBasic` / `logout` 비활성화
- `SessionCreationPolicy.STATELESS`
- permitAll: `/api/health`, `/api/users/social/**`, `/api/users/token/refresh`
  (인증 엔드포인트만 열고 `/api/users/**`로 넓히지 않습니다. 이후 추가되는 회원 API가 자동으로 공개되는 것을 막기 위함입니다)
- 그 외 모든 요청은 인증 필요
- `oauth2ResourceServer.jwt()` — Bearer 토큰 파싱·검증은 프레임워크가 담당하므로 커스텀 필터가 없습니다
- 헤더는 기본값 유지 — `X-Frame-Options: DENY`

### h2-console 전용 체인 (local 프로파일 전용)

h2-console 경로는 `@Profile("local")` + `@Order(0)`이 붙은 별도 체인이 담당합니다.
이 체인에서만 permitAll과 `frameOptions sameOrigin`(콘솔이 프레임을 쓰기 때문)을 허용합니다.
local 외 프로파일에서는 이 빈 자체가 등록되지 않아 콘솔 경로도 메인 체인의
`anyRequest().authenticated()`에 걸려 401이며, 콘솔 서블릿도 등록되지 않습니다
(`spring.h2.console.enabled`는 `application-local.yml`에만 있습니다).
프로파일 구성은 [profiles.md](profiles.md)를 참고하세요.

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

카카오가 OIDC로 전환된 뒤로 Kakao·Google·Apple이 **모두 "JWKS 디코더 + `iss` + `exp` + `aud`" 동일 형태**입니다.
`KakaoSocialProfileReader`를 그대로 베끼고 설정값만 바꾸면 됩니다.

### Google (예정)

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
| `KAKAO_AUDIENCES` | 허용할 id_token `aud` 목록(쉼표 구분). 네이티브 앱 키, 필요하면 REST API 키. 시크릿이 아닌 식별자. **빈 값이면 기동 실패** (비어 있으면 모든 로그인이 aud 불일치로 거부되므로) |
| `DEV_LOGIN_TOKEN` | local·dev 로그인 토큰. 32자 이상이며 dev에만 주입(local은 기본값 제공) |

로컬은 환경 변수로 export하고, 배포는 ECS task definition의 환경변수와 Secrets Manager 참조로 주입합니다.
테스트는 `src/test/resources/application-test.yml`의 더미 값을 쓰므로 환경 변수가 필요 없습니다.
dev/prod의 DB 접속 변수(`DB_HOST` 등)는 [profiles.md](profiles.md)를 참고하세요.

## 알려진 제약

- **최초 로그인 동시성** — 같은 신규 계정으로 동시에 두 요청이 오면 `(provider, provider_id)`
  유니크 제약 위반으로 한쪽이 500이 될 수 있습니다. 확률이 낮아 MVP에서는 두고, 필요 시
  제약 위반을 잡아 재조회하도록 보완합니다.
- **카카오 이메일은 대체로 null입니다.** `account_email` 동의 항목은 비즈니스 앱 심사가 필요하고,
  사용자가 동의를 거부할 수도 있습니다. 동의가 없으면 id_token에 `email` 클레임 자체가 없습니다.
  `SocialAccount.email`이 nullable이라 동작에는 문제가 없습니다.
- **앱이 OIDC를 켜야 합니다.** 카카오 개발자 콘솔에서 OpenID Connect를 활성화하고 앱이 `openid`
  스코프로 로그인해야 id_token이 내려옵니다. 액세스 토큰만 보내면 `USER_INVALID_SOCIAL_TOKEN`입니다.
- **공개 키 조회 실패는 여전히 로그인을 막습니다.** 디코더가 JWKS를 캐시하므로 매 로그인이
  카카오에 묶이지는 않지만, 캐시가 비어 있을 때 조회에 실패하면
  `USER_SOCIAL_PROVIDER_UNAVAILABLE`(502)로 토큰 무효(401)와 구분해 응답합니다.
  `spring.http.clients.read-timeout`(3초)이 최후 방어선입니다.
