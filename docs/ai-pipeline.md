# AI 리포트 파이프라인

영상 업로드부터 AI 분석 리포트 저장까지는 코드만 봐서는 이어지지 않는 비동기 흐름입니다. 조각별 구현은 코드가 원천이고, 이 문서는 흐름과 설계 결정만 기록합니다.

## 전체 흐름

```
앱 → POST /api/ai/presigned-urls           (ai — media의 VideoUploadUrlIssuer로 presigned URL 발급)
앱 → S3에 영상 직접 업로드                   (서버를 거치지 않음)
S3 → ObjectCreated 이벤트 → SQS 큐          (meongcoach.ai.video-queue)
VideoUploadSqsConsumer                      (ai/adapter/consumer)
  → AiReportGenerator.generate(objectKey)   (AiReportGenerateService)
     → VideoDownloadUrlIssuer               (media — presigned 다운로드 URL + 소유자 ID)
     → AiReport PENDING 저장                 (소유자를 알게 된 즉시. 이후 결말은 이 row의 status로 기록)
     → AiTrialFinder                        (무료 체험 횟수 확인, 초과 시 FAILED_TRIAL_EXCEEDED)
     → VideoAnalyzer → EvoLink 영상 분석     (ai/adapter/integration, 실패 시 FAILED_ANALYSIS)
     → ReportTitleGenerator → 제목 생성       (실패해도 제목 없이 진행)
     → AiReport COMPLETED 전이 저장           (그 외 예외는 FAILED_UNEXPECTED)
```

앱은 리포트 목록·상세의 `status`를 폴링하다가 `PENDING`이 아니면 멈춥니다.

소유자 식별은 객체 키 경로에 인코딩되어 있습니다 — `videos/{대상}/{userId}/{UUID}.{확장자}` 규칙은 `media/domain/vo/VideoObjectKey`가 단일 소유합니다.

## 설계 결정과 함정

- **컨슈머는 어떤 예외도 리스너 밖으로 내보내지 않습니다.** SQS는 리스너가 예외를 던지면 메시지를 재전달하는데, MVP라 재시도 로직이 없어 무한 재전달이 됩니다. 재시도가 필요해지면 예외를 삼키는 정책부터 다시 설계해야 합니다. (`VideoUploadSqsConsumer` 주석 참고)
- **실패는 error 로그와 함께 같은 `AiReport` row의 `status`로 남깁니다.** 실패가 DB에 흔적을 남기지 않으면 앱 폴링에 종료 조건이 없기 때문입니다. 소유자를 확인한 직후 PENDING row를 만들고, 체험 초과는 `FAILED_TRIAL_EXCEEDED`, 분석 실패는 `FAILED_ANALYSIS`, 그 뒤의 예상 밖 예외는 `FAILED_UNEXPECTED`로 전이합니다. 실패 기록 자체가 실패하면 로그만 남기고 끝냅니다. 예외는 객체 키 형식 위반(`InvalidVideoObjectKeyException`) 하나뿐입니다 — 소유자를 알 수 없어 row 없이 컨슈머가 warn 로그로 버립니다.
- **`AiReportGenerateService`는 의도적으로 트랜잭션이 없습니다.** 영상 분석이 수십 초 걸려 클래스 기본 `@Transactional(readOnly = true)` 패턴을 따르면 분석 내내 DB 커넥션을 점유합니다. 저장과 상태 전이는 리포지토리 `save`(준영속 엔티티는 merge → UPDATE)의 자체 트랜잭션에 맡깁니다. "서비스 클래스에 readOnly 기본" 규칙의 의도적 예외이므로 일괄 정리 대상이 아닙니다.
- **무료 체험 횟수는 `COMPLETED` 리포트만 셉니다.** (`countByUserIdAndStatus`) 실패·진행 중 row는 체험을 소모하지 않으므로, 분석이 실패한 사용자는 다시 업로드할 수 있습니다.
- **`ai_reports` 스키마 변경(status 컬럼, content NOT NULL 해제)은 dev·prod에 수동 DDL로 반영합니다.** 정책과 이유는 [profiles.md](profiles.md)의 ddl-auto 절이 원천입니다.
- **중복 분석은 `existsByVideoObjectKey`로 막습니다.** SQS는 at-least-once 전달이라 같은 이벤트가 두 번 올 수 있습니다. 상태를 가리지 않으므로 PENDING·FAILED row에도 걸려 같은 영상은 재분석되지 않습니다. 따라서 분석 도중 앱이 죽으면 row가 PENDING으로 남고 재전달 메시지는 스킵됩니다 — 재시도를 도입할 때 함께 풀어야 할 한계입니다.
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
