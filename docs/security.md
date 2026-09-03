# 인증·보안

소셜 로그인으로 회원을 식별하고, 이후 모든 API 인가는 **자체 발급 JWT**로 합니다.
스토어 심사용 테스트 계정만 예외적으로 이메일·비밀번호 로그인을 쓰며, 발급되는 JWT는 같습니다. ([이메일 로그인](#이메일-로그인-스토어-심사용-테스트-계정) 참고)

## 로그인 흐름 — 네이티브 SDK + 서버 검증

클라이언트가 React Native 앱이므로, 서버가 리다이렉트를 주고받는 OAuth2 인가 코드 흐름을 쓰지 않습니다.
**서버가 제공자의 REST 키·시크릿을 보관하지 않습니다.** 서버가 아는 제공자 값은 우리 앱을 가리키는 공개 식별자(`aud`)뿐입니다.
지원 제공자는 카카오(`kakao`)·구글(`google`)·애플(`apple`)이며, 모두 OIDC id_token을 받습니다.

```
[앱] 제공자 SDK 네이티브 로그인 (카카오 SDK / Google Sign-In / Sign in with Apple, OIDC)
  → 제공자 id_token 획득 (애플은 identityToken)
     → POST /api/auth/login/social/{provider}  { "token": "..." }
        → 서버가 id_token 서명·발급자·만료·aud 검증 (캐시된 공개 키로 로컬 검증)
           → 회원 조회·생성 (User + SocialAccount)
              → 우리 JWT 발급
        ← { accessToken, refreshToken, needsOnboarding }
```

제공자가 발급한 토큰은 **로그인 시점에만 쓰고 버립니다.** 이후 인가는 전적으로 우리 JWT로 합니다.
제공자 토큰은 "제공자 API를 호출할 권한"이지 "우리 서비스를 쓸 권한"이 아니고, 우리 권한 정보를 담을 수도 없기 때문입니다.

## aud 검증 — 생략하면 안 되는 이유

id_token의 서명이 유효하다는 것은 "제공자가 발급했다"만 증명하고,
**"우리 앱에 발급했다"는 증명하지 않습니다.**

검증이 없으면 공격자가 자신의 카카오·구글·애플 앱에서 받은 유효한 토큰을 우리 서버에 제출해
**해당 제공자 사용자로 로그인할 수 있습니다** (confused deputy).

그래서 `OidcIdTokenVerifier`는 id_token의 `aud`가 설정된
`meongcoach.social.{provider}.audiences`에 포함되는지 대조하고, 아니면 거부합니다
(`USER_SOCIAL_TOKEN_APP_MISMATCH`). `aud`가 아예 없는 토큰도 같은 이유로 거부합니다.

`aud`는 플랫폼마다 다릅니다 — 카카오는 **네이티브 앱은 네이티브 앱 키, 웹은 REST API 키**,
구글은 **안드로이드·서버 검증은 웹 클라이언트 ID, iOS는 iOS 클라이언트 ID**(안드로이드용 OAuth 클라이언트 ID는
SHA-1 검증용이라 id_token의 `aud`가 되지 않습니다), 애플은 **iOS 네이티브는 앱 번들 ID, 웹·안드로이드는 Services ID**입니다.
그래서 단일 값이 아니라 목록으로 받습니다. 여기에는 반드시 우리 앱의 식별자만 넣어야 합니다.

`aud` 검증만 디코더의 `OAuth2TokenValidator` 체인이 아니라 `verify()`에서 직접 합니다.
검증기에 넣으면 서명 실패와 같은 `JwtValidationException`으로 뭉개져
`USER_INVALID_SOCIAL_TOKEN`과 구분할 수 없기 때문입니다. 이 검증은 별도 에러 코드를 유지할 값어치가 있습니다.

## 토큰 정책

| 항목 | 값 |
|---|---|
| 서명 | HS256 (대칭키) |
| 액세스 토큰 | 1시간 |
| 리프레시 토큰 | 14일 |
| 클레임 | `iss`, `sub`(회원 ID), `iat`, `exp`, `token_type`, `jti`(리프레시 토큰만) |
| 저장 | **하지 않음.** 토큰 자체는 서명·만료·용도로만 검증하고, `sub`가 등록된 회원인지만 DB로 확인 |

### 액세스 토큰 검증 순서

1. 서명(HS256) — 우리 비밀키로 서명되었는가
2. `exp` (`JwtTimestampValidator`) — 만료되지 않았는가
3. `iss` (`JwtIssuerValidator`) — 우리가 발급했는가
4. `token_type` (`TokenTypeValidator`) — 액세스 토큰 자리에 맞는 용도인가
5. `sub` (`UserRoleAuthenticationConverter`) — 등록된 회원인가 확인하며 DB에서 역할을 읽어 `ROLE_*` 권한 부여. 미등록이면 401

5번이 없으면 회원 행이 사라진 뒤에도(예: DB 초기화) 남아 있는 토큰이 만료 전까지 그대로 통과하고,
각 모듈이 존재하지 않는 `userId`로 조회·저장을 시도하게 됩니다. 회원 조회가 필요해 이 컨버터만
`shared`가 아니라 `user` 모듈(`user/adapter/security/UserRoleAuthenticationConverter`)에 둡니다 —
`shared`가 `user`를 참조하면 순환 의존이 되므로 `SecurityConfig`는 `Converter<Jwt, AbstractAuthenticationToken>`
타입으로만 받습니다. 비용은 인증 요청당 PK 조회 한 번입니다.

역할을 JWT 클레임이 아니라 요청마다 DB에서 읽는 이유: 온보딩 완료로 `ONBOARDING_MEMBER → MEMBER`
승격이 일어나도 이미 발급된 액세스 토큰이 최대 1시간 낡은 역할을 들고 다니는 문제가 없고,
클라이언트가 토큰을 재발급받을 필요도 없습니다. 어차피 5번이 요청당 PK 조회를 하고 있었으므로
추가 비용도 없습니다.

> 함정: 이 컨버터는 `@Component`가 아니라 `UserSecurityConfig`의 `@Bean`으로 등록합니다.
> `Converter` 구현 컴포넌트는 `@WebMvcTest` 슬라이스의 로드 대상에 포함되어,
> 모든 컨트롤러 슬라이스 테스트가 회원 조회 의존성을 요구하며 깨지기 때문입니다.

### URL 인가 규칙

첫 번째로 매칭된 규칙이 이기므로 **선언 순서가 결정적**입니다.

| 순서 | 경로 | 접근 |
|---|---|---|
| 1 | `/api/health`, `/api/auth/login/social/**`, `/api/auth/login/local`, `/api/auth/token/refresh` | permitAll |
| 2 | `/swagger-ui/**` | 문서 활성 환경 permitAll, 그 외 denyAll |
| 3 | `/api/onboarding/**`, `/api/dogs/profile/image` | `MEMBER`, `ONBOARDING_MEMBER` |
| 4 | `DELETE /api/users/me` | `MEMBER`, `ONBOARDING_MEMBER` |
| 5 | 그 외 전부 | `MEMBER` |

3번은 온보딩 화면에 필요한 경로입니다 — 온보딩 완료 요청에 프로필 이미지 URL이 들어가므로
기본 프로필 이미지 조회는 온보딩 중에도 열려 있어야 합니다. 이미지 업로드 presigned URL 발급은
`POST /api/onboarding/presigned-urls`라 `/api/onboarding/**`에 이미 포함됩니다.
`GUEST`는 어떤 규칙에도 매칭되지 않아 모든 보호 경로에서 403이며, 아직 발급 경로가 없습니다.

4번은 스토어 심사관이 온보딩을 마치지 않은 채 탈퇴를 시험할 수 있어서 둔 예외입니다. `DELETE` 메서드만 열어,
같은 경로에 나중에 생길 회원 조회·수정이 온보딩 회원에게 함께 열리지 않게 합니다.

역할 어휘의 단일 원천은 `shared/security/AuthorityRole`입니다. `TokenType`과 같은 부류
(영속되지 않는 보안 어휘)라서 shared에 두고, `user/domain/UserRole`(영속 도메인 상태)이
선언부 매핑으로 이 어휘를 참조합니다 — `shared`가 `user`를 참조할 수 없어 의존을 역전한 것입니다.
`SecurityConfig`와 `GlobalExceptionHandler`도 같은 enum을 쓰므로 문자열 드리프트가 생길 수 없고,
잘못된 매핑은 `UserTest`의 어휘 매핑 검증이 잡습니다.

### 액세스/리프레시 디코더 분리

`SecurityConfig`는 디코더를 두 개 정의하고 `@Primary`를 두지 않습니다. 각 디코더에
`TokenTypeValidator`가 붙어 `token_type` 클레임을 강제하므로, **리프레시 토큰을 `Authorization: Bearer`로
제출하면 401**입니다. 이 검증이 없으면 리프레시 토큰이 사실상 14일짜리 액세스 토큰이 됩니다.

회원 존재 검증은 액세스 디코더에만 붙습니다. 재발급 경로는 `TokenRefreshService`가 같은 확인을 하고
`USER_INVALID_REFRESH_TOKEN`(401)으로 응답해, 클라이언트가 재로그인 분기를 그대로 쓸 수 있게 합니다.

### 무상태 정책의 트레이드오프

리프레시 토큰을 저장하지 않으므로 **강제 로그아웃·토큰 무효화가 불가능합니다.**
탈취된 리프레시 토큰은 만료(14일)까지 유효합니다.

회원 존재 검증은 **탈퇴 회원도 미등록으로 취급**합니다(`RegisteredUserCheckService`). 탈퇴(`WITHDRAWN`)는 상태만 바뀌고
행이 남는 soft delete라 존재 여부만 보면 만료까지 토큰이 통과하기 때문입니다. 액세스 토큰 인증(`UserRoleAuthenticationConverter`)과
재발급(`TokenRefreshService`)이 모두 이 서비스를 거치므로, 토큰을 저장하지 않고도 탈퇴 즉시 두 경로가 401로 끝납니다.
이는 특정 회원의 토큰 전부를 거부하는 것이라 토큰 단위 무효화(강제 로그아웃)와는 다릅니다.

완화 장치:
- 액세스 토큰 수명을 1시간으로 짧게 유지
- 필요 시 `refresh-token-validity` 단축

강제 로그아웃이 요구사항이 되면 리프레시 토큰 영속화가 필요하며, 이는 정책 변경입니다.

## 필터 체인 구성

리다이렉트 흐름이 없어 세션이 필요 없으므로 **무상태 체인 하나**를 기본으로 둡니다.

- `csrf` / `formLogin` / `httpBasic` / `logout` 비활성화
- `SessionCreationPolicy.STATELESS`
- permitAll: `/api/health`, `/api/auth/login/social/**`, `/api/auth/login/local`, `/api/auth/token/refresh`
  (인증 엔드포인트만 개별 경로로 열고 `/api/auth/**`로 넓히지 않습니다. 이후 추가되는 인증 관련 API가 자동으로 공개되는 것을 막기 위함입니다)
- 그 외 요청은 역할 기반 인가 (위 "URL 인가 규칙" 참고)
- `oauth2ResourceServer.jwt()` — Bearer 토큰 파싱·검증은 프레임워크가 담당하므로 커스텀 필터가 없습니다.
  회원 존재 확인·권한 부여도 커스텀 필터가 아니라 디코더 뒤의 컨버터에 얹습니다 (위 "액세스 토큰 검증 순서" 참고)
- `cors` — 프로파일별 `meongcoach.cors.allowed-origin-patterns`의 origin만 허용합니다 (허용 메서드: GET, POST, PUT, PATCH, DELETE, OPTIONS).
  CORS 필터가 체인 앞단에서 동작하므로 preflight는 인가 전에 처리되고, 401 응답에도 CORS 헤더가 실립니다.
  허용 목록 바인딩은 `shared/security/CorsProperties`, 빈 정의는 `SecurityConfig`에 둡니다.
- 헤더는 기본값 유지 — `X-Frame-Options: DENY`

### 인증 실패 응답

`AuthenticationException`·`AccessDeniedException`은 `DispatcherServlet` 밖(필터 체인)에서 던져져
`@RestControllerAdvice`가 잡지 못합니다. `SecurityExceptionTranslator`가 이 예외를 Spring MVC의
`handlerExceptionResolver`로 되돌려 보내면 `GlobalExceptionHandler`가 처리하므로,
**Problem Details 형식을 만드는 곳은 여전히 한 군데뿐**입니다.

에러 코드는 `UNAUTHORIZED` / `FORBIDDEN` — "프레임워크 예외는 HTTP 상태 enum 이름" 규칙 그대로입니다.
인증 실패 원인은 공격자에게 힌트가 되므로 `detail`에 일반화된 문구만 담습니다.

예외가 하나 있습니다: 온보딩 미완료 회원(`ONBOARDING_MEMBER`)이 정회원 전용 경로에 접근해 생긴
403은 클라이언트가 온보딩 화면으로 분기해야 하므로, `GlobalExceptionHandler`가 `SecurityContext`의
권한을 보고 `ONBOARDING_NOT_COMPLETED` 코드로 구분해 응답합니다. `SecurityExceptionTranslator`의
재디스패치가 같은 스레드에서 일어나 `SecurityContext`가 살아 있기에 가능한 방식입니다 —
비동기 디스패치를 도입하면 재검토가 필요합니다.

> 트레이드오프: 리소스 서버 기본 엔트리 포인트를 교체하므로 `WWW-Authenticate: Bearer` 헤더가 사라집니다.
> 모바일 전용 API라 수용했습니다.

## 이메일 로그인 (스토어 심사용 테스트 계정)

Google Play·App Store 심사자는 소셜 계정을 만들 수 없으므로, 심사용 테스트 계정만 이메일·비밀번호로 로그인합니다.
**회원가입·이메일 인증·비밀번호 변경 API는 없습니다.** 계정(`LocalAccount`)은 운영자가 DB에 직접 등록하며 생성 이후 수정되지 않습니다.

```
[앱] 이메일·비밀번호 입력
  → POST /api/auth/login/local  { "email": "...", "password": "..." }
     → LocalAccountRepository.findByEmail → PasswordEncoder.matches(BCrypt) → 탈퇴 여부 확인
        → 우리 JWT 발급 (소셜 로그인과 동일)
  ← { accessToken, refreshToken, needsOnboarding }
```

비밀번호는 `BCryptPasswordEncoder`(빈 정의는 `SecurityConfig`) 해시로만 저장합니다. `domain`은 Spring에 의존할 수 없어
`LocalAccount`는 해시 문자열만 보관하고, 대조는 `application/LocalLoginService`가 합니다.

### 실패 응답 정책

| 상황 | 응답 |
|---|---|
| 이메일 미등록 **또는** 비밀번호 불일치 | 401 `USER_INVALID_CREDENTIALS` — 어느 쪽인지 구분하지 않습니다. 구분하면 계정 존재 여부가 드러납니다 |
| 이메일 형식 오류 | 400 `USER_INVALID_EMAIL` — 계정 존재와 무관한 입력 형식 검증이라 정보가 새지 않습니다. 형식 규칙은 `Email` 값 객체 한 곳에만 둡니다 |
| 탈퇴한 회원 | 403 `USER_WITHDRAWN` — **비밀번호 대조를 통과한 뒤에만** 확인해, 비밀번호를 모르는 쪽에 탈퇴 여부가 드러나지 않게 합니다 |

### 테스트 계정 등록

**local**은 `src/main/resources/db/local/test-account-data.sql`이 기동마다 적재합니다(이메일·비밀번호는 파일 머리 주석 참고).
온보딩 전 상태(`ONBOARDING_MEMBER`, 프로필 없음)로 두어 심사자가 앱에서 온보딩까지 직접 진행합니다.

**dev/prod**는 마이그레이션 도구가 없어 psql로 직접 등록합니다. 먼저 BCrypt 해시를 만듭니다.

```bash
htpasswd -bnBC 10 "" '비밀번호' | tr -d ':\n'   # macOS 기본 제공. $2y$ 접두어도 BCryptPasswordEncoder가 허용합니다
```

그 다음 회원 행과 계정 행을 같은 트랜잭션으로 넣습니다. (`User` Javadoc의 규칙 — 회원 생성은 자격증명 생성과 같은 트랜잭션에서만)

```sql
BEGIN;
WITH new_user AS (
	INSERT INTO users (role, status, created_at, updated_at)
	VALUES ('ONBOARDING_MEMBER', 'ACTIVE', now(), now()) RETURNING id
)
INSERT INTO local_accounts (user_id, email, password_hash, created_at, updated_at)
SELECT id, 'review@example.com', '$2y$10$...', now(), now() FROM new_user;
COMMIT;
```

- 평문 비밀번호는 커밋하지 않고 심사 제출 양식에만 적습니다. local 시드의 비밀번호는 로컬 전용이라 예외입니다.
- 심사가 끝나 계정을 막으려면 `UPDATE users SET status = 'WITHDRAWN' WHERE id = (SELECT user_id FROM local_accounts WHERE email = '...')`.
  행을 지우려면 `local_accounts` → `users` 순서로 삭제합니다.
- **심사관이 탈퇴 API(`DELETE /api/users/me`)를 시험하면 `local_accounts` 행이 삭제되어 그 계정으로는 더 로그인할 수 없습니다.**
  `users` 행은 `WITHDRAWN`으로 남지만 이메일 유니크는 풀리므로, 위 SQL로 같은 이메일을 다시 등록하면 됩니다(새 `users` 행이 생깁니다).
  심사 제출 전과 심사 사이에 계정이 살아 있는지 확인하세요. local은 기동마다 시드가 다시 적재되어 신경 쓸 필요가 없습니다.
- prod는 `ddl-auto: validate`라 `local_accounts` 테이블이 없으면 기동 자체가 실패합니다. 엔티티는 이미 배포되어 있으므로 테이블 존재만 확인하면 됩니다.

## 제공자 추가 방법

`application/required`의 `SocialProfileReader`가 확장 지점입니다.

```java
public interface SocialProfileReader {
	SocialProvider provider();
	SocialAccountLinkCommand read(String credential);
}
```

`SocialLoginService`가 `List<SocialProfileReader>`를 주입받아 `provider()` 기준 맵으로 만듭니다.
**제공자 추가 = `{제공자}Properties` record + `{제공자}SocialProfileReader` `@Component` + 설정 블록. 기존 클래스 수정은 없습니다.**

OIDC 제공자는 모두 "JWKS 디코더 + `iss` + `exp` + `aud`" 동일 형태라, 검증은 `user/adapter/integration/OidcIdTokenVerifier`
하나가 맡고 제공자별 리더는 설정 배선과 클레임 매핑만 합니다. `{제공자}Properties`가 `OidcProviderProperties`
(`issuer`, `jwkSetUri`, `audiences`)를 구현하면 검증기에 그대로 넘길 수 있습니다. 살아있는 예시는 `AppleSocialProfileReader`.

## 환경 변수

기본값을 두지 않아 미설정 시 **기동에 실패합니다.** 커밋된 개발용 키가 배포 환경으로 흘러가는 것을 막기 위함입니다.

| 변수 | 설명 |
|---|---|
| `JWT_SECRET` | JWT 서명 키. **32바이트 이상** (미달 시 기동 실패) |
| `KAKAO_NATIVE_APP_KEY` | 네이티브 SDK id_token의 `aud`. 시크릿이 아닌 식별자. **빈 값이면 기동 실패** |
| `KAKAO_REST_API_KEY` | 웹 로그인 id_token의 `aud`. 시크릿이 아닌 식별자. **빈 값이면 기동 실패** |
| `APPLE_BUNDLE_ID` | Sign in with Apple id_token의 `aud`(iOS 앱 번들 ID). 시크릿이 아닌 식별자. **빈 값이면 기동 실패** |
| `GOOGLE_WEB_CLIENT_ID` | 구글 id_token의 `aud`(웹 OAuth 클라이언트 ID). 안드로이드 SDK도 이 값을 `aud`로 발급합니다. 시크릿이 아닌 식별자. **빈 값이면 기동 실패** |
| `GOOGLE_IOS_CLIENT_ID` | 구글 id_token의 `aud`(iOS OAuth 클라이언트 ID). 시크릿이 아닌 식별자. **빈 값이면 기동 실패** |

로컬은 환경 변수로 export하고, 배포는 ECS task definition의 환경변수와 Secrets Manager 참조로 주입합니다.
테스트는 `src/test/resources/application-test.yml`의 더미 값을 쓰므로 환경 변수가 필요 없습니다.
dev/prod의 DB 접속 변수(`DB_HOST` 등)는 [profiles.md](profiles.md)를 참고하세요.

## 알려진 제약

- **탈퇴 시 소셜 제공자 연결을 끊지 않습니다.** 애플 심사 가이드라인 5.1.1(v)은 Sign in with Apple 앱이 계정 삭제 시
  `https://appleid.apple.com/auth/revoke`로 토큰을 revoke하도록 요구하지만, 서버는 id_token만 검증하고 `.p8` 키·client_secret·
  authorization code 수신 경로가 없어 아직 구현하지 않았습니다. 구현하려면 `APPLE_TEAM_ID`·`APPLE_KEY_ID`·`APPLE_PRIVATE_KEY`로
  ES256 client_secret을 만들고, 클라이언트가 탈퇴 요청에 authorization code를 실어 보내 `/auth/token` 교환 → `/auth/revoke` 순으로
  호출해야 합니다. 카카오 unlink(`/v1/user/unlink`)와 구글 revoke(`https://oauth2.googleapis.com/revoke`)도 같은 이유로
  미구현입니다. 그동안 사용자 기기의 설정 > Apple ID > Apple로 로그인 목록과 구글 계정의 연결된 앱 목록에는 앱이 남습니다.
- **탈퇴해도 타 모듈 데이터는 남습니다.** 강아지(`dogs`)·AI 리포트(`ai_reports`)·학습 진도는 옛 `userId`로 남으며, 재가입은 새
  `userId`를 받으므로 도달할 수 없는 고아 행이 됩니다. 삭제용 `provided` 인터페이스나 모듈 이벤트 선례가 없어 MVP에서는 두고,
  필요해지면 `UserWithdrawer`에서 각 모듈의 정리 인터페이스를 호출하도록 넓힙니다.
- **이메일 로그인은 응답 시간을 균등화하지 않습니다.** 이메일이 없으면 BCrypt 대조 없이 바로 401이라, 응답 시간으로
  등록 여부를 추정할 수 있습니다. 로컬 계정은 심사용 몇 개뿐이라 MVP에서는 수용하며, 필요해지면 미존재 분기에서도
  더미 해시에 `matches`를 한 번 호출해 균등화합니다.
- **역할 도입 전 가입자** — `UserRole` 도입 전 `MEMBER`로 만들어진 회원 중 프로필이 없는 계정은
  온보딩을 마치지 않고도 전 API에 접근할 수 있습니다(`needsOnboarding`은 여전히 true).
  온보딩을 완료하면 멱등 승격으로 일관성이 회복되므로 MVP에서는 수용합니다.
- **최초 로그인 동시성** — 같은 신규 계정으로 동시에 두 요청이 오면 `(provider, provider_id)`
  유니크 제약 위반으로 한쪽이 500이 될 수 있습니다. 확률이 낮아 MVP에서는 두고, 필요 시
  제약 위반을 잡아 재조회하도록 보완합니다.
- **카카오 이메일은 대체로 null입니다.** `account_email` 동의 항목은 비즈니스 앱 심사가 필요하고,
  사용자가 동의를 거부할 수도 있습니다. 동의가 없으면 id_token에 `email` 클레임 자체가 없습니다.
  `SocialAccount.email`이 nullable이라 동작에는 문제가 없습니다.
- **앱이 OIDC를 켜야 합니다.** 카카오 개발자 콘솔에서 OpenID Connect를 활성화하고 앱이 `openid`
  스코프로 로그인해야 id_token이 내려옵니다. 액세스 토큰만 보내면 `USER_INVALID_SOCIAL_TOKEN`입니다.
- **애플 이메일은 비공개 릴레이 주소일 수 있습니다.** 사용자가 "이메일 가리기"를 고르면 `email`이
  `@privaterelay.appleid.com` 주소로 오고, 이메일 공유에 동의하지 않으면 클레임 자체가 없습니다.
  카카오와 마찬가지로 `SocialAccount.email`이 nullable이라 동작에는 문제가 없습니다. 이름은 id_token에
  없고 최초 인가 응답에만 실리므로 서버는 받지 않습니다.
- **애플·구글 id_token의 `nonce`는 검증하지 않습니다.** 서버가 nonce를 발급·보관하는 왕복이 없는 무상태
  설계라 카카오와 같은 기준을 적용합니다. 재사용 창은 id_token 만료(애플 10분, 구글 1시간)로 제한됩니다.
- **구글 `iss`는 `https://accounts.google.com` 하나만 허용합니다.** 구글 문서상 `accounts.google.com`(스킴 없음)으로도
  올 수 있다고 되어 있지만 현행 Google Sign-In SDK는 `https://` 형태로 발급하며, `OidcProviderProperties.issuer`를
  목록으로 넓히면 카카오·애플 설정과 검증기까지 함께 바뀌어야 해서 단일 값으로 둡니다. 스킴 없는 `iss`가 실제로
  관측되면 그때 넓힙니다.
- **공개 키 조회 실패는 여전히 로그인을 막습니다.** 디코더가 JWKS를 캐시하므로 매 로그인이
  제공자에 묶이지는 않지만, 캐시가 비어 있을 때 조회에 실패하면
  `USER_SOCIAL_PROVIDER_UNAVAILABLE`(502)로 토큰 무효(401)와 구분해 응답합니다.
  `spring.http.clients.read-timeout`(3초)이 최후 방어선입니다.
