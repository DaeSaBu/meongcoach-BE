# AI 리포트 파이프라인

영상 업로드부터 AI 분석 리포트 저장까지는 코드만 봐서는 이어지지 않는 비동기 흐름입니다. 조각별 구현은 코드가 원천이고, 이 문서는 흐름과 설계 결정만 기록합니다.

## 전체 흐름

```
앱 → POST /api/ai/presigned-urls           (ai — media의 VideoUploadUrlIssuer로 presigned URL 발급)
  → AiReport UPLOADING 저장                 (발급 즉시. uploadExpiresAt = 발급 시각 + URL 유효 시간. 응답에 reportId 포함)
앱 → S3에 영상 직접 업로드                   (서버를 거치지 않음)
S3 → ObjectCreated 이벤트 → SQS 큐          (meongcoach.ai.video-queue)
VideoUploadSqsConsumer                      (ai/adapter/consumer)
  → AiReportGenerator.generate(objectKey)   (AiReportGenerateService)
     → findByVideoObjectKey                 (발급 row 없으면 warn 후 스킵, UPLOADING이 아니면 중복 전달로 스킵)
     → VideoDownloadUrlIssuer               (media — presigned 다운로드 URL)
     → AiReport PENDING 전이 저장             (이후 결말은 이 row의 status로 기록)
     → AiTrialFinder                        (무료 체험 횟수 확인, 초과 시 FAILED_TRIAL_EXCEEDED)
     → VideoAnalyzer → EvoLink 영상 분석     (ai/adapter/integration, 실패 시 FAILED_ANALYSIS)
     → ReportTitleGenerator → 제목 생성       (실패해도 제목 없이 진행)
     → AiReport COMPLETED 전이 저장           (그 외 예외는 FAILED_UNEXPECTED)
```

앱은 `GET /api/ai/reports`를 폴링하다가 `UPLOADING`·`PENDING`인 리포트가 없으면 멈춥니다. 사용자 ID만으로 묻는 조회라 앱이 재시작돼 객체 키·리포트 ID를 잃어도 폴링을 이어갈 수 있습니다 — 그래서 row를 업로드 완료가 아니라 **발급 시점**에 만듭니다.

소유자 식별은 객체 키 경로에 인코딩되어 있습니다 — `videos/{대상}/{userId}/{UUID}.{확장자}` 규칙은 `media/domain/vo/VideoObjectKey`가 단일 소유합니다. 리포트의 소유자는 발급 시 row에 기록한 `userId`가 원천입니다.

## 설계 결정과 함정

- **컨슈머는 어떤 예외도 리스너 밖으로 내보내지 않습니다.** SQS는 리스너가 예외를 던지면 메시지를 재전달하는데, MVP라 재시도 로직이 없어 무한 재전달이 됩니다. 재시도가 필요해지면 예외를 삼키는 정책부터 다시 설계해야 합니다. (`VideoUploadSqsConsumer` 주석 참고)
- **실패는 error 로그와 함께 같은 `AiReport` row의 `status`로 남깁니다.** 실패가 DB에 흔적을 남기지 않으면 앱 폴링에 종료 조건이 없기 때문입니다. 컨슈머는 발급 row를 PENDING으로 전이한 뒤, 체험 초과는 `FAILED_TRIAL_EXCEEDED`, 분석 실패는 `FAILED_ANALYSIS`, 그 뒤의 예상 밖 예외는 `FAILED_UNEXPECTED`로 전이합니다. 실패 기록의 저장이 실패하면 `FAILED_UNEXPECTED`로 한 번 더 시도하고, 그마저 실패하면 예외가 컨슈머로 나가 error 로그 후 버려집니다(row는 PENDING으로 남음). 예외는 객체 키 형식 위반(`InvalidVideoObjectKeyException`) 하나뿐입니다 — PENDING 전이 전에 나므로 row는 UPLOADING으로 남아 만료 뒤 `FAILED_UPLOAD`로 조회됩니다.
- **업로드되지 않은 발급 row는 조회 시점에 `FAILED_UPLOAD`로 파생합니다.** 발급만 받고 업로드하지 않은 row가 UPLOADING으로 영영 남으면 앱 폴링이 끝나지 않습니다. 발급 시 저장한 `uploadExpiresAt`(URL 유효 시간)이 지난 UPLOADING은 `AiReport.statusAt(now)`가 `FAILED_UPLOAD`로 돌려줍니다. DB 상태는 바꾸지 않습니다(스케줄러·쓰기 없음) — 유효 시간을 넘겨 끝난 긴 업로드의 이벤트가 늦게 오면 컨슈머가 그대로 PENDING으로 전이하므로, 그 사이 잠깐 `FAILED_UPLOAD`로 보이는 것이 유일한 부작용입니다. 상태로 SQL 필터링(예: `?status=`)이 필요해지면 파생이 아니라 저장으로 바꿔야 합니다.
- **발급 기록이 없는 업로드 이벤트는 분석하지 않습니다.** 우리가 발급한 키만 소유자 row가 있습니다. 배포 직전에 발급돼 배포 뒤 업로드된 영상도 row가 없어 스킵되므로(warn 로그), 배포 시 URL 유효 시간만큼의 공백을 감수합니다.
- **EvoLink 어댑터는 연동 실패를 도메인 예외로 번역해 던지고, 서비스는 그 둘만 잡습니다.** 영상 분석은 `VideoAnalysisFailedException`(→ `FAILED_ANALYSIS`), 제목 생성은 `ReportTitleGenerationFailedException`(→ 제목 없이 `COMPLETED`). 그 외 예외는 버그로 보고 `FAILED_UNEXPECTED`로 남깁니다. 어댑터에서 `catch (Exception)`으로 넓게 잡으면 두 실패 상태의 구분이 무너지므로 유지하지 않습니다.
- **`AiReportGenerateService`는 의도적으로 트랜잭션이 없습니다.** 영상 분석이 수십 초 걸려 클래스 기본 `@Transactional(readOnly = true)` 패턴을 따르면 분석 내내 DB 커넥션을 점유합니다. 저장과 상태 전이는 리포지토리 `save`(준영속 엔티티는 merge → UPDATE)의 자체 트랜잭션에 맡깁니다. "서비스 클래스에 readOnly 기본" 규칙의 의도적 예외이므로 일괄 정리 대상이 아닙니다.
- **무료 체험 횟수는 `COMPLETED` 리포트만 셉니다.** (`countByUserIdAndStatus`) 실패·진행 중 row는 체험을 소모하지 않으므로, 분석이 실패한 사용자는 다시 업로드할 수 있습니다.
- **`ai_reports` 스키마 변경(status 컬럼, content NOT NULL 해제, upload_expires_at 컬럼)은 dev·prod에 수동 DDL로 반영합니다.** 정책과 이유는 [profiles.md](profiles.md)의 ddl-auto 절이 원천입니다.
- **중복 분석은 발급 row의 상태로 막습니다.** SQS는 at-least-once 전달이라 같은 이벤트가 두 번 올 수 있습니다. 컨슈머는 `findByVideoObjectKey`로 찾은 row가 UPLOADING일 때만 분석을 시작하므로 PENDING·COMPLETED·FAILED row는 재분석되지 않습니다. 따라서 분석 도중 서버가 죽으면 row가 PENDING으로 남고 재전달 메시지는 스킵됩니다 — 재시도를 도입할 때 함께 풀어야 할 한계입니다.
- **제목 200자 규칙은 `AiReport` 도메인이 단일 소유합니다** (`TITLE_MAX_LENGTH`). 프롬프트나 어댑터에서 길이를 중복 강제하지 않습니다. 제목 생성 실패는 부가 정보 실패라 리포트 저장을 막지 않습니다.

## EvoLink 연동 (ai/adapter/integration)

- OpenAI 호환 chat API를 씁니다. 멀티모달(영상) 호출은 `api.evolink.ai`가 주력 엔드포인트입니다.
- **응답은 `json_schema` + `strict`로 강제합니다.** 모델이 스키마를 엄격 준수해야 어댑터의 정제 로직을 줄일 수 있습니다. (`EvoLinkChatRequest.ResponseFormat`)
- **`responseTimeout`은 전역 HTTP read-timeout(3초)과 별개입니다.** 영상 분석은 수십 초가 걸리므로 전역 값을 줄이거나 이 값을 없애면 분석이 전부 실패합니다. 설정 항목별 의미는 `EvoLinkProperties` javadoc이 원천입니다.
- 실제 EvoLink 호출이 필요한 수동 테스트는 로컬 전용입니다. (`EvoLinkVideoAnalyzerManualTest`)

## 프롬프트 템플릿 (src/main/resources/prompts/)

`{video-analysis,report-title}/{system.md, user.md, schema.json}` 구조입니다.

- `PromptLoader`가 기동 시점에 읽어 검증합니다 — 파일이 없거나 비어 있으면 **애플리케이션이 기동하지 않습니다.**
- `schema.json`은 strict 모드로 응답 형태를 강제하므로, 프롬프트에서 출력 형식을 바꾸려면 스키마를 함께 수정해야 합니다.

## 필요 설정

`application.yml`의 `spring.cloud.aws`(SQS 자격 증명)와 `meongcoach.ai` 블록이 원천이며, 각 항목의 이유는 해당 yml 주석에 있습니다. 배포 Secrets 주입은 [cd-dev.yml](../.github/workflows/cd-dev.yml)·[cd-prod.yml](../.github/workflows/cd-prod.yml)이 원천입니다.
