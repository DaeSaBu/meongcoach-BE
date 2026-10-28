# AI 리포트 파이프라인

영상 업로드부터 AI 분석 리포트 저장까지는 코드만 봐서는 이어지지 않는 비동기 흐름입니다. 조각별 구현은 코드가 원천이고, 이 문서는 흐름과 설계 결정만 기록합니다.

## 전체 흐름

```
앱 → POST /api/ai/presigned-urls           (ai — media의 MediaUploadUrlIssuer로 GCS V4 서명 URL 발급)
  → AiReport UPLOADING 저장                 (발급 즉시. uploadExpiresAt = 발급 시각 + URL 유효 시간. 응답에 reportId 포함)
앱 → GCS에 영상 직접 업로드                  (서버를 거치지 않음)
GCS → OBJECT_FINALIZE 알림 → Pub/Sub 토픽    (버킷 알림 설정, videos/training/ 접두사)
VideoUploadSubscriber                       (ai/adapter/consumer — streaming pull 구독, meongcoach.ai.pubsub.video-subscription)
  → VideoUploadPubSubConsumer               (attributes의 eventType·objectId만 읽는다)
  → AiReportGenerator.generate(objectKey)   (AiReportGenerateService)
     → findByVideoObjectKey                 (발급 row 없으면 warn 후 스킵, UPLOADING이 아니면 중복 전달로 스킵)
     → MediaStorageUriResolver              (media — gs://버킷/키)
     → AiReport PENDING 전이 저장             (이후 결말은 이 row의 status로 기록)
     → AiTrialFinder                        (무료 체험 횟수 확인, 초과 시 FAILED_TRIAL_EXCEEDED)
     → VideoAnalyzer → Gemini 영상 분석      (ai/adapter/integration — files:register로 gs:// 등록 후 generateContent, 실패 시 FAILED_ANALYSIS)
     → ReportTitleGenerator → 제목 생성       (EvoLink, 실패해도 제목 없이 진행)
     → AiReport COMPLETED 전이 저장           (그 외 예외는 FAILED_UNEXPECTED)
```

앱은 `GET /api/ai/reports`를 폴링하다가 `UPLOADING`·`PENDING`인 리포트가 없으면 멈춥니다. 사용자 ID만으로 묻는 조회라 앱이 재시작돼 객체 키·리포트 ID를 잃어도 폴링을 이어갈 수 있습니다 — 그래서 row를 업로드 완료가 아니라 **발급 시점**에 만듭니다.

소유자 식별은 객체 키 경로에 인코딩되어 있습니다 — `videos/training/{userId}/{UUID}.{확장자}` 구조는 `media/domain/MediaObjectKey`가 단일 소유하고, 경로 범위 `training`은 `ai/domain/AiVideo`가 정합니다. 리포트의 소유자는 발급 시 row에 기록한 `userId`가 원천입니다.

## 설계 결정과 함정

- **컨슈머는 어떤 예외도 밖으로 내보내지 않고 항상 ack합니다.** Pub/Sub은 ack하지 않은 메시지를 재전달하는데, MVP라 재시도 로직이 없어 무한 재전달이 됩니다. 재시도가 필요해지면 예외를 삼키는 정책부터 다시 설계해야 합니다. (`VideoUploadPubSubConsumer`)
- **구독은 메시지를 한 번에 하나씩만 받습니다.** 영상 분석이 수 분 걸리므로 `VideoUploadSubscriber`가 흐름 제어를 1건으로 묶어 직렬 처리합니다. ack 기한은 streaming pull이 자동 연장합니다(기본 최대 60분). 처리량이 문제가 되면 ECS 태스크 수가 아니라 이 값을 먼저 봅니다.
- **GCS 알림은 attributes만 믿습니다.** 객체 키는 `objectId` attribute에 URL 인코딩 없이 그대로 오고, 이벤트 종류는 `eventType`입니다. 본문(JSON_API_V1 객체 메타데이터)은 읽지 않습니다. `OBJECT_FINALIZE`가 아닌 이벤트(삭제·메타데이터 변경)는 무시합니다.
- **실패는 error 로그와 함께 같은 `AiReport` row의 `status`로 남깁니다.** 실패가 DB에 흔적을 남기지 않으면 앱 폴링에 종료 조건이 없기 때문입니다. 컨슈머는 발급 row를 PENDING으로 전이한 뒤, 체험 초과는 `FAILED_TRIAL_EXCEEDED`, 분석 실패는 `FAILED_ANALYSIS`, 그 뒤의 예상 밖 예외는 `FAILED_UNEXPECTED`로 전이합니다. 실패 기록의 저장이 실패하면 `FAILED_UNEXPECTED`로 한 번 더 시도하고, 그마저 실패하면 예외가 컨슈머로 나가 error 로그 후 버려집니다(row는 PENDING으로 남음). 예외는 객체 키 형식 위반(`InvalidMediaObjectKeyException`) 하나뿐입니다 — PENDING 전이 전에 나므로 row는 UPLOADING으로 남아 만료 뒤 `FAILED_UPLOAD`로 조회됩니다.
- **업로드되지 않은 발급 row는 조회 시점에 `FAILED_UPLOAD`로 파생합니다.** 발급만 받고 업로드하지 않은 row가 UPLOADING으로 영영 남으면 앱 폴링이 끝나지 않습니다. 발급 시 저장한 `uploadExpiresAt`(URL 유효 시간)이 지난 UPLOADING은 `AiReport.statusAt(now)`가 `FAILED_UPLOAD`로 돌려줍니다. DB 상태는 바꾸지 않습니다(스케줄러·쓰기 없음) — 유효 시간을 넘겨 끝난 긴 업로드의 이벤트가 늦게 오면 컨슈머가 그대로 PENDING으로 전이하므로, 그 사이 잠깐 `FAILED_UPLOAD`로 보이는 것이 유일한 부작용입니다. 상태로 SQL 필터링(예: `?status=`)이 필요해지면 파생이 아니라 저장으로 바꿔야 합니다.
- **발급 기록이 없는 업로드 이벤트는 분석하지 않습니다.** 우리가 발급한 키만 소유자 row가 있습니다. 배포 직전에 발급돼 배포 뒤 업로드된 영상도 row가 없어 스킵되므로(warn 로그), 배포 시 URL 유효 시간만큼의 공백을 감수합니다.
- **어댑터는 연동 실패를 도메인 예외로 번역해 던지고, 서비스는 그 둘만 잡습니다.** 영상 분석은 `VideoAnalysisFailedException`(→ `FAILED_ANALYSIS`), 제목 생성은 `ReportTitleGenerationFailedException`(→ 제목 없이 `COMPLETED`). 그 외 예외는 버그로 보고 `FAILED_UNEXPECTED`로 남깁니다. 어댑터에서 `catch (Exception)`으로 넓게 잡으면 두 실패 상태의 구분이 무너지므로 유지하지 않습니다.
- **`AiReportGenerateService`는 의도적으로 트랜잭션이 없습니다.** 영상 분석이 수십 초 걸려 클래스 기본 `@Transactional(readOnly = true)` 패턴을 따르면 분석 내내 DB 커넥션을 점유합니다. 저장과 상태 전이는 리포지토리 `save`(준영속 엔티티는 merge → UPDATE)의 자체 트랜잭션에 맡깁니다. "서비스 클래스에 readOnly 기본" 규칙의 의도적 예외이므로 일괄 정리 대상이 아닙니다.
- **무료 체험 횟수는 `COMPLETED` 리포트만 셉니다.** (`countByUserIdAndStatus`) 실패·진행 중 row는 체험을 소모하지 않으므로, 분석이 실패한 사용자는 다시 업로드할 수 있습니다.
- **`ai_reports` 스키마 변경(status 컬럼, content NOT NULL 해제, upload_expires_at 컬럼)은 dev·prod에 수동 DDL로 반영합니다.** 정책과 이유는 [profiles.md](profiles.md)의 ddl-auto 절이 원천입니다.
- **중복 분석은 발급 row의 상태로 막습니다.** Pub/Sub은 at-least-once 전달이라 같은 이벤트가 두 번 올 수 있습니다(GCS 알림 자체도 중복될 수 있음). 컨슈머는 `findByVideoObjectKey`로 찾은 row가 UPLOADING일 때만 분석을 시작하므로 PENDING·COMPLETED·FAILED row는 재분석되지 않습니다. 따라서 분석 도중 서버가 죽으면 row가 PENDING으로 남고 재전달 메시지는 스킵됩니다 — 재시도를 도입할 때 함께 풀어야 할 한계입니다.
- **제목 200자 규칙은 `AiReport` 도메인이 단일 소유합니다** (`TITLE_MAX_LENGTH`). 프롬프트나 어댑터에서 길이를 중복 강제하지 않습니다. 제목 생성 실패는 부가 정보 실패라 리포트 저장을 막지 않습니다.
- **추천 교육은 모델이 `topicId`를 돌려주고 서버가 교육 목록에 있는 id인지 검증합니다.** 교육 이름은 카테고리 간에 중복되므로("앉아"가 101·1101) 이름→id 역매핑이 불가능합니다. 그래서 프롬프트의 교육 목록에 id를 함께 넣고 응답 스키마에 `topicId`를 required로 둡니다. 목록에 없는 id는 `VideoAnalysisFailedException`(→ `FAILED_ANALYSIS`)이고, 도입 전에 저장된 리포트의 `topicId`는 null입니다.

## Gemini 연동 (영상 분석)

- **영상은 GCS 객체를 `files:register`로 등록해 `gs://` URI로 넘깁니다.** Gemini Developer API가 영상을 받는 방법 중 외부 HTTPS·서명 URL 직접 입력은 파일당 15MB 상한이라 50MiB 영상에 못 쓰고, Files API 업로드는 서버가 영상을 내려받아 다시 올려야 합니다. 등록은 데이터 이동 없이 Gemini가 버킷에서 직접 읽습니다. (`GeminiVideoAnalyzer.registerOrThrow`)
- **등록 호출은 API 키가 아니라 서비스 계정 OAuth 토큰을 씁니다.** `GoogleCredentials`(shared/config/GoogleCloudConfig — `cloud-platform`·`devstorage.read_only` 스코프, 프로젝트를 quota project로 지정)를 그대로 넘기고, 분석 호출(`generateContent`)만 API 키입니다. 서비스 계정에 `roles/serviceusage.serviceUsageConsumer`가 없으면 등록이 403으로 실패합니다.
- **버킷에는 Gemini API 서비스 에이전트의 `roles/storage.objectViewer`가 있어야 합니다.** 프로젝트마다 `gcloud beta services identity create --service=generativelanguage.googleapis.com`으로 만든 에이전트에 부여합니다. 권한이 없으면 등록은 되지만 분석에서 읽기 실패가 납니다. 등록 한 번으로 최대 30일간 접근이 유지되고, 영상은 분석 직후에만 쓰므로 재등록·만료는 고려하지 않습니다.
- **응답은 `responseMimeType: application/json` + `responseJsonSchema`(`schema.json` 그대로)로 강제합니다.** Gemini는 JSON Schema 부분집합을 지원해 `additionalProperties: false`·`required`가 통하지만 `minLength` 같은 일부 키워드는 무시되므로 길이 규칙은 도메인(`AiReport`)에서 검증합니다.
- **종료 사유로 실패를 구분합니다.** `MAX_TOKENS`는 상한 잘림, `STOP`이 아닌 그 외(`SAFETY`·`PROHIBITED_CONTENT` 등)는 차단, `candidates` 없음(프롬프트 차단)도 모두 `VideoAnalysisFailedException`입니다. SDK의 `text()`는 `SAFETY`에서 `IllegalArgumentException`을 던지므로 종료 사유를 먼저 확인합니다.
- **사고 수준은 `thinking-level`(low·medium·high)로 둡니다.** Gemini 3 이후 모델은 사고를 완전히 끌 수 없고 `thinkingBudget`은 문서화되어 있지 않습니다. 비용·지연 예측을 위해 기본값은 `low`입니다.
- **`responseTimeout`은 전역 HTTP read-timeout(3초)과 별개입니다.** SDK `HttpOptions.timeout`(밀리초)에 넣어 등록·분석 호출 모두에 적용됩니다.
- Gemini 클라이언트는 OkHttp로 호출해 `MockRestServiceServer`로 잡을 수 없으므로 테스트는 `HttpOptions.baseUrl`을 `MockWebServer`로 돌립니다. (`GeminiVideoAnalyzerTest`)
- 실제 Gemini·GCS 호출이 필요한 수동 테스트는 로컬 전용입니다. (`*ManualTest`는 gitignore)

## EvoLink 연동 (제목·플랜 생성)

- 다른 모듈은 `ai/application/provided`의 `AiContentGenerator`로 시스템 프롬프트, 사용자 프롬프트, 응답 JSON 스키마를 전달합니다. `EvoLinkContentGenerator`가 스키마를 `JsonNode` 객체로 변환하고 내부 클라이언트를 호출합니다. 생성된 내용은 문자열로 반환하며 업무별 결과 해석과 검증은 호출 모듈이 담당합니다.
- JSON 문법이 잘못되거나 객체가 아닌 스키마는 외부 호출 전에 거절합니다. 통신 오류, 빈 응답, 콘텐츠 차단, 출력 잘림은 원인을 보존한 `AiContentGenerationFailedException`으로 변환하며 오류 코드는 `AI_CONTENT_GENERATION_FAILED`(502)입니다. 로컬 스키마 오류는 내부 오류(500)로 처리합니다.
- `ai/adapter/integration` 내부의 `EvoLinkChatClient`가 인증, HTTP 전송, 타임아웃과 공통 응답 검증을 담당합니다. 요청과 응답의 전송 DTO도 같은 패키지 아래에 둡니다.
- 연결 설정(`baseUrl`, `apiKey`, `responseTimeout`)과 모델 설정(`model`, `maxTokens`, `temperature`, `thinking`)은 AI 모듈 내부의 `EvoLinkProperties`가 함께 읽습니다. 기존 `meongcoach.ai.evolink` 설정 키는 유지합니다.
- OpenAI 호환 chat API를 씁니다. **엔드포인트는 `direct.evolink.ai`입니다.** `api.evolink.ai`는 Cloudflare 뒤에 있어 응답 첫 바이트가 100초를 넘으면 `524`로 끊깁니다.
- **응답은 `json_schema` + `strict`로 강제합니다.** (`EvoLinkChatRequest.ResponseFormat`)

## 프롬프트 템플릿 (src/main/resources/prompts/)

`{video-analysis,report-title,plan-generation}/{system.md, user.md, schema.json}` 구조입니다.

- `PromptLoader`가 기동 시점에 읽어 검증합니다 — 파일이 없거나 비어 있으면 **애플리케이션이 기동하지 않습니다.**
- `schema.json`은 응답 형태를 강제하므로, 프롬프트에서 출력 형식을 바꾸려면 스키마를 함께 수정해야 합니다. 영상 분석 스키마는 Gemini `responseJsonSchema`와 EvoLink `json_schema`가 공통으로 받는 JSON Schema 부분집합만 씁니다.

## 필요 설정

`application.yml`의 `meongcoach.google-cloud`(서비스 계정), `meongcoach.storage.gcs`(버킷), `meongcoach.ai.pubsub`(구독), `meongcoach.ai.gemini`(모델) 블록이 원천이며, 각 항목의 이유는 해당 yml 주석에 있습니다. 배포 Secrets 주입은 [cd-dev.yml](../.github/workflows/cd-dev.yml)·[cd-prod.yml](../.github/workflows/cd-prod.yml)이 원천입니다.

Google Cloud 쪽에는 프로젝트(dev·prod 분리)마다 버킷, Pub/Sub 토픽과 pull 구독, 버킷 알림(`OBJECT_FINALIZE`, 접두사 `videos/training/`), 서비스 계정(버킷 `storage.objectAdmin`·구독 `pubsub.subscriber`·프로젝트 `serviceusage.serviceUsageConsumer`), Gemini 서비스 에이전트의 버킷 `storage.objectViewer`, Generative Language API로 제한한 API 키가 있어야 합니다.
