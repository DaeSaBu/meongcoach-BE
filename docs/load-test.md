# 부하 테스트 (JMeter Java DSL)

부하 테스트 시나리오를 저장소에 코드로 두어 팀원 누구나 같은 조건으로 재현하고 diff로 리뷰할 수 있게 합니다.
`.jmx` XML 대신 [jmeter-java-dsl](https://abstracta.github.io/jmeter-java-dsl/)로 시나리오를 Java로 쓰고, 실행 결과로 JMeter HTML 대시보드와 `.jmx`(GUI에서 열어볼 용도)를 함께 남깁니다.

## 구성과 결정

| 항목 | 내용 |
|---|---|
| 소스 위치 | `src/loadTest/java`(별도 Gradle 소스셋). `./gradlew test`·CI·JaCoCo 어디에도 연결되지 않고 `./gradlew loadTest`로만 실행 |
| 실행 JDK | **JDK 21**. JMeter 5.6.3이 번들한 Groovy 3.0.20이 Java 23+ 클래스 파일을 읽지 못해([apache/jmeter#6402](https://github.com/apache/jmeter/issues/6402)) 이 소스셋만 21로 컴파일·실행합니다. 로컬에 없으면 foojay 리졸버(`settings.gradle.kts`)가 내려받습니다. `-PloadTestJava=25`로 실험할 수 있습니다 |
| Groovy 금지 | 시나리오에서 `__groovy`·Groovy JSR223를 쓰지 않습니다. 조건은 JMeter 내장 `__jexl3`, 유일값은 `__UUID`로 만듭니다 |
| 대상 환경 | dev(ECS)가 기본 목적이며 base URL만 바꾸면 로컬 `bootRun`에도 그대로 돌아갑니다 |
| 계정 생성 | 앱에는 가입 API가 없어 `loadtest` 모듈의 `POST /api/loadtest/accounts`로 만듭니다. `meongcoach.loadtest.enabled`가 true인 local·dev에서만 열리고 공유 키 헤더 `X-Loadtest-Key`로 보호합니다([profiles.md](profiles.md), [security.md](security.md)) |
| 데이터 준비 | 시드 SQL 대신 실제 API로 만듭니다 — 계정 생성 → 로그인 → `POST /api/onboarding`(정회원 승격, 강아지 1마리) → 필요 시 `POST /api/ai/presigned-urls`(UPLOADING 리포트 1건). 그래서 dev에서도 사전 작업 없이 실행됩니다 |

## 시나리오

| 클래스 (`src/loadTest/java/.../scenario`) | 요청 | 측정 의도 |
|---|---|---|
| `AccountCreateLoadTest` | `POST /api/loadtest/accounts` | insert 2건(users·local_accounts) + BCrypt 해싱. 인증 불필요 |
| `TrainingCategoriesLoadTest` | `GET /api/training/categories` | 카테고리·토픽 전량 반환. 응답 페이로드가 가장 큼 |
| `LessonCardsLoadTest` | `GET /api/training/lessons/{id}/cards` | 카드+미디어 컬렉션 중첩 조회. 레슨 ID는 `-Dloadtest.lessonId`, 없으면 첫 커리큘럼의 첫 레슨을 API로 찾음 |
| `DogsLoadTest` | `GET /api/dogs` | 컬럼이 가장 많은 엔티티 + `@EntityGraph` 컬렉션 fetch join |
| `TokenRefreshLoadTest` | `POST /api/auth/token/refresh` | `refresh_tokens` SELECT·UPDATE·INSERT. 리프레시 토큰은 1회용(rotation)이라 스레드마다 계정을 독점하고 응답의 새 토큰을 다음 요청에 씀 |
| `AiReportPollingLoadTest` | `GET /api/ai/reports/{reportId}` | 앱이 분석 완료를 기다리며 폴링하는 경로. 계정마다 자기 리포트를 반복 조회 |

모든 인증 요청은 `UserRoleAuthenticationConverter`가 `users`를 한 번 더 읽으므로(역할 확인) GET 부하에는 그 SELECT 1회가 항상 포함됩니다.

## 실행

```bash
# 로컬: ./gradlew bootRun(compose postgres)을 먼저 띄운다. 키 기본값은 local-loadtest-key
./gradlew loadTest --tests '*TrainingCategoriesLoadTest' \
  -Dloadtest.baseUrl=http://localhost:8080 -Dloadtest.apiKey=local-loadtest-key \
  -Dloadtest.threads=20 -Dloadtest.rampUp=10s -Dloadtest.duration=60s

# dev: 환경 변수 방식도 같다(LOADTEST_BASE_URL, LOADTEST_API_KEY, LOADTEST_THREADS …)
LOADTEST_BASE_URL=https://api.dev.meongcoach.com LOADTEST_API_KEY=… LOADTEST_THREADS=50 \
  ./gradlew loadTest --tests '*TokenRefreshLoadTest'

# 전체 시나리오를 차례로
./gradlew loadTest -Dloadtest.baseUrl=… -Dloadtest.apiKey=…
```

| 파라미터 (`-D` / 환경 변수) | 기본값 | 의미 |
|---|---|---|
| `loadtest.baseUrl` / `LOADTEST_BASE_URL` | `http://localhost:8080` | 대상 서버 |
| `loadtest.apiKey` / `LOADTEST_API_KEY` | 없음 | 계정 생성 공유 키. 인증 시나리오도 계정을 처음 만들 때 필요 |
| `loadtest.threads` | 10 | 동시 사용자 수 |
| `loadtest.rampUp` | `10s` | 스레드를 모두 띄우는 데 걸리는 시간 (`30s`, `2m`, `PT1M30S`) |
| `loadtest.duration` | `60s` | 목표 스레드 수에 도달한 뒤 유지하는 시간 |
| `loadtest.accounts` | `threads` | 준비할 계정 수. rotation 시나리오 때문에 `threads` 이상이어야 함 |
| `loadtest.lessonId` | 자동 탐색 | 레슨 카드 시나리오의 레슨 ID |

- IntelliJ에서 시나리오 클래스를 직접 실행하면 `:test` 태스크로 위임되어 "no tests found"가 납니다. 항상 `./gradlew loadTest --tests`로 실행하세요.
- `-D` 값은 Gradle이 포크한 JVM에 다시 넣어주는 목록(`build.gradle.kts`의 `loadTest` 태스크)에 있는 키만 전달됩니다. 파라미터를 추가하면 그 목록도 갱신하세요.

## 산출물

```
build/load-test/
  accounts.csv                        # 이메일·비밀번호. 실행 간 재사용(gitignore: build/)
  <scenario>/<yyyyMMdd-HHmmss>/
    tokens.csv                        # 이번 실행의 토큰·리포트 ID (민감 — build/ 아래라 커밋되지 않음)
    plan.jmx                          # JMeter GUI: File > Open. CSV 경로가 절대경로라 같은 PC에서 바로 재생 가능
    results.jtl                       # 원본 샘플 로그
    report/<시각 uuid>/index.html     # JMeter HTML 대시보드
```

실행이 끝나면 콘솔에 샘플 수·오류 수·p50/p99·대시보드 경로를 한 줄로 출력합니다. `errors > 0`이면 JUnit 단정으로 실패합니다.

## 정리

계정 생성 시나리오는 요청마다 계정을 남기고, 인증 시나리오도 `accounts.csv`만큼의 계정·강아지·리포트를 만듭니다.
dev DB에서는 [load-test/cleanup-loadtest-accounts.sql](../load-test/cleanup-loadtest-accounts.sql)로 이메일 `lt-…@meongcoach.test` 계정과 하위 행을 지웁니다.
로컬은 재기동(`create-drop`)으로 초기화됩니다. `accounts.csv`를 지우면 다음 실행이 계정을 새로 만듭니다.

## 함정

- **액세스 토큰은 1시간** 만료라 준비 단계가 매 실행 새로 발급합니다. `tokens.csv`를 다른 실행에 재사용하지 마세요.
- **rotation 시나리오에서 한 번 실패하면** 그 스레드는 폐기된 토큰으로 401이 연쇄됩니다. 리포트는 "첫 실패 이후 연쇄"로 읽으면 되고, 이것이 rotation이 정상 동작한다는 뜻입니다.
- **AI 리포트는 체험 횟수 제한**이 있어 준비 단계는 기존 리포트가 있으면 재사용하고 없을 때만 만듭니다.
- **HTML 리포트 폴더는 비어 있어야** 해서 실행마다 타임스탬프 폴더를 새로 만듭니다. 오래된 폴더는 `./gradlew clean`이나 수동으로 지웁니다.
- **JMeter 임시 홈**은 `build/load-test/tmp` 아래에 생기고 JVM 종료 시 삭제됩니다. 저장소 루트에 파일이 남으면 `loadTest` 태스크의 `workingDir` 설정을 의심하세요.
