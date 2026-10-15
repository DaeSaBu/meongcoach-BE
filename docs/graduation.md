# 토픽 수료(실습 인증)

토픽의 모든 커리큘럼을 마친 회원이 대표 실습 영상을 올리면 운영자가 판정하고, 승인된 토픽마다 전체 이용권 3일을 보상으로 줍니다.
화면 문구는 `실습 인증`·`토픽 수료`이고, 코드·API·테이블 이름은 `graduation`입니다. 기획 원문은 기획 저장소의 `practice-certification-plan.md`입니다.

## 전체 흐름

```
앱: POST /api/training/topics/{topicId}/graduation/presigned-urls  → presigned PUT URL (DB 변경 없음)
앱: S3에 영상 PUT (videos/graduation/{userId}/{uuid}.mp4)
앱: POST /api/training/topics/{topicId}/graduation/submissions      → 제출 생성 + Slack 알림
운영자: GET /api/admin/graduations/submissions?result=PENDING        → 대기 목록과 영상 URL
운영자: PUT /api/admin/graduations/submissions/{submissionId}/review → 승인 또는 보완 요청
앱: GET /api/training/topics/{topicId}/graduation                    → 상태 확인(포커스마다 재조회)
앱: PUT /api/training/topics/{topicId}/graduation/reward             → RevenueCat 프로모션 지급 + 이용권 동기화
```

업로드 완료를 S3 이벤트로 감지하지 않습니다. 앱이 업로드를 마친 뒤 제출을 만듭니다. 같은 버킷의 S3→SQS 이벤트는
AI 분석 소비자(`VideoUploadSqsConsumer`)가 `videos/training/` 접두사만 처리하므로 실습 영상은 무시됩니다.

## 데이터

| 테이블 | 단위 | 내용 |
|---|---|---|
| `topic_graduations` | 회원·토픽당 1행(`uk_topic_graduations_user_id_topic_id`) | 지금 상태(`SUBMITTED`·`REVISION_REQUESTED`·`APPROVED`), 승인 시각, 보상 받은 시각·만료 시각 |
| `graduation_submissions` | 제출마다 1행(`uk_graduation_submissions_graduation_id_attempt_no`) | 영상 키, 결과(`PENDING`·`APPROVED`·`REVISION_REQUESTED`·`REPLACED`), 판정 시각, 운영자 메모 |

이름이 비슷한 `topic_progress`(progress 모듈)는 회원당 1행으로 "지금 배우고 있는 토픽"을 가리키므로 단위가 다릅니다.
토픽의 "학습 완료"는 저장하지 않고 `TopicCompletionChecker`가 레슨 진도로 매번 계산합니다. 커리큘럼이 1개 이상이고 모든 레슨을 완료해야 완료입니다.

### 수료 사실의 출처

"이 회원이 이 토픽을 수료했는가"는 `topic_graduations.status = APPROVED` 하나로만 판단합니다. 다른 모듈(뱃지, 토픽 목록의 수료 표시, 수료증)은
이 사실을 따로 저장하지 않고 `GraduationFinder.isApproved`·`findApprovedTopicIds`로 읽습니다. 뱃지의 지급 기록처럼 파생 기록을 남기는 것은 괜찮지만,
토픽 단위의 수료 상태를 적는 두 번째 테이블은 만들지 않습니다.

## 상태 전이

- 업로드 URL 발급은 DB를 바꾸지 않습니다. 토픽 미완료면 403(`GRADUATION_TOPIC_NOT_COMPLETED`), 이미 수료면 409(`GRADUATION_ALREADY_APPROVED`)입니다.
- 제출은 수료 행을 쓰기 잠금(`findForUpdate`)으로 읽고, 없으면 만듭니다. 상태를 `SUBMITTED`로 바꾸고, 검수 대기 제출이 있으면 `REPLACED`로 남긴 뒤 다음 번호의 `PENDING` 제출을 추가합니다.
- 같은 토픽의 첫 제출 두 건이 동시에 들어오면 둘째는 유니크 제약에 걸리고 409(`GRADUATION_SUBMISSION_CONFLICT`)로 바뀝니다. 앱은 다시 조회하면 됩니다.
- 제출 키는 `videos/graduation/{본인 userId}/{이름}.{확장자}` 형식이어야 합니다. 아니면 400(`GRADUATION_INVALID_VIDEO_KEY`)입니다. S3에 객체가 실제로 있는지는 확인하지 않습니다.
- 판정은 수료 행을 먼저 잠근 뒤 제출을 읽어, 같은 수료의 제출·교체와 겹치지 않게 합니다. `PENDING`이 아니면 409(`GRADUATION_SUBMISSION_NOT_PENDING`), 보완 요청에 메모가 없으면 400(`GRADUATION_REVIEW_NOTE_REQUIRED`)입니다.

## 보상

- 승인된 수료 한 건당 한 번, 네 종류(`PUPPY`·`JUNIOR`·`ADULT`·`SENIOR`)를 같은 만료 시각으로 지급합니다. 기한은 없습니다.
- 만료 시각은 `max(지금, 이 회원이 이전에 받은 보상의 가장 늦은 만료 시각) + 3일`입니다(`RewardPolicy`).
- 지급은 `EntitlementGranter`가 RevenueCat v2 `POST /projects/{projectId}/customers/{userId}/actions/grant_entitlement`(본문 `entitlement_id`, `expires_at` 밀리초)를 종류마다 부르고, 이어서 이용권 동기화로 서버 DB에 반영합니다. 서버 DB에만 넣으면 다음 동기화가 RevenueCat에 없는 이용권을 만료시키기 때문입니다.
- RevenueCat에 고객이 없으면(앱에서 구매 SDK를 한 번도 초기화하지 않은 회원) 404가 오므로, `POST /projects/{projectId}/customers`로 고객을 만든 뒤 한 번 다시 지급합니다.
- RevenueCat은 같은 이용권을 더 늦은 만료 시각으로 다시 지급하면 연장하지만, 기존 지급의 만료와 2시간 이내로 가까우면 중복으로 보고 무시합니다. 보상은 3일 단위로 이어 붙으므로 문제가 되지 않습니다.
- 네 종류가 모두 성공하고 동기화까지 끝난 뒤에만 받은 시각을 기록합니다. 도중에 실패하면 502(`ENTITLEMENT_PROVIDER_UNAVAILABLE`)이고 받지 않은 상태로 남으며, 다시 누르면 그 시점 기준으로 다시 계산해 네 종류를 다시 지급합니다.
- 보상 받기는 회원 ID advisory lock(`pg_advisory_xact_lock`)으로 직렬화합니다. 이용권 동기화와 같은 키라 같은 트랜잭션 안에서 다시 잡아도 막히지 않고, 두 토픽을 동시에 받아도 3일씩 이어 붙습니다.
- 이미 받았으면 지급하지 않고 저장된 값을 돌려줍니다(멱등).

## 운영 런북

1. 제출이 들어오면 `SLACK_GRADUATION_WEBHOOK_URL`로 한 줄이 옵니다. 예: `[실습 인증 제출] 토픽 #7 분리불안 · userId 42 · submissionId 31 · 2번째 제출 · 2026-10-14T14:03`(한국 시간). 전송 실패는 경고 로그만 남기고 제출은 그대로 성공합니다.
2. 대기 목록을 받아 `videoUrl`(1시간 유효)로 영상을 봅니다.
   ```bash
   curl -s -H "X-Admin-Key: $ADMIN_API_KEY" "https://<api>/api/admin/graduations/submissions?result=PENDING"
   ```
3. 판정합니다. 메모는 앱에 그대로 보이므로 사용자에게 말하듯 적습니다(최대 500자). 보완 요청은 메모가 필수입니다.
   ```bash
   curl -s -X PUT -H "X-Admin-Key: $ADMIN_API_KEY" -H "Content-Type: application/json" \
     -d '{"result":"REVISION_REQUESTED","note":"보호자가 현관 밖으로 나가는 장면이 보이지 않아요. 그 장면부터 다시 찍어 주세요."}' \
     https://<api>/api/admin/graduations/submissions/31/review
   ```
4. 판정 기준은 "실습을 시도한 장면이 확인되면 승인"입니다. 토픽과 무관한 영상도 별도 반려 없이 보완 요청으로 처리하고 메모에 적습니다.
5. 운영자는 보상을 따로 지급하지 않습니다. 승인하면 회원이 앱에서 `받기`를 눌러 받습니다. 결과 알림(푸시)도 보내지 않습니다.

## 필요 설정

| 변수 | 설명 |
|---|---|
| `ADMIN_API_KEY` | 운영자 API의 `X-Admin-Key`. 32자 이상 랜덤. 비어 있으면 `/api/admin/**` 전체가 404 ([security.md](security.md)) |
| `SLACK_GRADUATION_WEBHOOK_URL` | 제출 알림용 Slack incoming webhook. 비어 있으면 알림을 보내지 않음 |
| `REVENUECAT_API_KEY` | `customer_information:customers:read_write` 권한이 있는 v2 secret 키. 읽기 권한만 있으면 보상 받기가 502로 실패 |

## 알려진 제약

- 교체·재제출로 밀려난 영상 파일은 지우지 않습니다. `videos/graduation/` 수명주기 규칙으로 정리하며, 지워진 뒤 이력의 영상을 열면 다운로드가 실패합니다.
- Slack 호출은 제출 트랜잭션 안에서 동기로 일어납니다(최악 읽기 타임아웃 3초). 트랜잭션이 롤백되면 알림만 나간 유령 제출이 생길 수 있습니다.
- 운영자 목록에 페이징이 없습니다.
