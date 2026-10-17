# 토픽 수료(실습 인증)

토픽의 모든 커리큘럼을 마친 회원이 대표 실습 영상을 올리면 운영자가 판정하고, 승인하는 순간 서버가 전체 이용권 3일을 지급합니다.
화면 문구는 `실습 인증`·`토픽 수료`이고, 코드·API·테이블 이름은 `graduation`입니다. 기획 원문은 기획 저장소의 `practice-certification-plan.md`입니다.

## 전체 흐름

```
앱: POST /api/training/topics/{topicId}/graduation/presigned-urls  → presigned PUT URL (DB 변경 없음)
앱: S3에 영상 PUT (videos/graduation/{userId}/{uuid}.mp4)
앱: POST /api/training/topics/{topicId}/graduation/submissions      → 수료 행 생성·갱신 + 기록 1줄 + Slack 알림(영상 링크 포함)
운영자: Slack 링크로 영상 확인
운영자: PUT /api/admin/graduations/{graduationId}/review             → 승인(이용권 지급 + 동기화) 또는 보완 요청
앱: GET /api/training/topics/{topicId}/graduation                    → 상태 확인(화면에 들어올 때마다 재조회)
```

업로드 완료를 S3 이벤트로 감지하지 않습니다. 앱이 업로드를 마친 뒤 제출합니다. 같은 버킷의 S3→SQS 이벤트는
AI 분석 소비자(`VideoUploadSqsConsumer`)가 `videos/training/` 접두사만 처리하므로 실습 영상은 무시됩니다.

## 데이터

| 테이블 | 단위 | 내용 |
|---|---|---|
| `topic_graduations` | 회원·토픽당 1행(`uk_topic_graduations_user_id_topic_id`) | 지금 상태(`SUBMITTED`·`REVISION_REQUESTED`·`APPROVED`), 가장 최근 제출의 차수·영상 키·제출 시각, 판정 시각·운영자 메모, 승인 시각, 보상 이용권 만료 시각 |
| `graduation_submissions` | 제출마다 1행(`uk_graduation_submissions_graduation_id_attempt_no`) | 차수, 영상 키, 제출 시각. 추가만 하고 서버는 읽지 않습니다. 이력은 SQL로 봅니다 |

이름이 비슷한 `topic_progress`(progress 모듈)는 회원당 1행으로 "지금 배우고 있는 토픽"을 가리키므로 단위가 다릅니다.
토픽의 "학습 완료"는 저장하지 않고 `TopicCompletionChecker`가 레슨 진도로 매번 계산합니다. 커리큘럼이 1개 이상이고 모든 레슨을 완료해야 완료입니다.

### 수료 사실의 출처

"이 회원이 이 토픽을 수료했는가"는 `topic_graduations.status = APPROVED` 하나로만 판단합니다. 다른 모듈(뱃지, 토픽 목록의 수료 표시, 수료증)은
이 사실을 따로 저장하지 않고 graduation 모듈의 provided 인터페이스로 읽습니다. 뱃지의 지급 기록처럼 파생 기록을 남기는 것은 괜찮지만,
토픽 단위의 수료 상태를 적는 두 번째 테이블은 만들지 않습니다.

## 상태 전이

- 업로드 URL 발급은 DB를 바꾸지 않습니다. 토픽 미완료면 403(`GRADUATION_TOPIC_NOT_COMPLETED`), 이미 수료면 409(`GRADUATION_ALREADY_APPROVED`)입니다.
- 제출은 수료 행을 쓰기 잠금(`findForUpdate`)으로 읽습니다. 없으면 1번째 제출로 만들고, 있으면 차수를 올리고 영상 키·제출 시각을 바꾸고 이전 판정 메모와 판정 시각을 지운 뒤 상태를 `SUBMITTED`로 돌립니다. 승인된 행이면 409(`GRADUATION_ALREADY_APPROVED`)입니다.
- 같은 토픽의 첫 제출 두 건이 동시에 들어오면 둘째는 유니크 제약에 걸리고 409(`GRADUATION_SUBMISSION_CONFLICT`)로 바뀝니다. 앱은 다시 조회하면 됩니다.
- 제출 키는 `videos/graduation/{본인 userId}/{이름}.{확장자}` 형식이어야 합니다. 아니면 400(`GRADUATION_INVALID_VIDEO_KEY`)입니다. S3에 객체가 실제로 있는지는 확인하지 않습니다.
- 판정은 수료 행을 id로 잠근 뒤 상태가 `SUBMITTED`인지 봅니다. 아니면 409(`GRADUATION_NOT_PENDING`), 행이 없으면 404(`GRADUATION_NOT_FOUND`), 보완 요청에 메모가 없으면 400(`GRADUATION_REVIEW_NOTE_REQUIRED`)입니다.

## 보상

- 승인할 때 서버가 바로 지급합니다. 회원이 따로 받기를 누르지 않고, 앱은 승인 화면을 열 때 이용권 동기화를 한 번 불러 잠금 표시만 새로 고칩니다.
- 승인된 수료 한 건당 한 번, 네 종류(`PUPPY`·`JUNIOR`·`ADULT`·`SENIOR`)를 같은 만료 시각으로 지급합니다.
- 만료 시각은 `max(승인 시각, 이 회원이 이전에 받은 보상의 가장 늦은 만료 시각) + 3일`입니다(`RewardPolicy`). 토픽 두 개를 연달아 승인하면 6일이 됩니다.
- 지급은 `EntitlementGranter`가 RevenueCat v2 `POST /projects/{projectId}/customers/{userId}/actions/grant_entitlement`(본문 `entitlement_id`, `expires_at` 밀리초)를 종류마다 부르고, 이어서 이용권 동기화로 서버 DB에 반영합니다. 서버 DB에만 넣으면 다음 동기화가 RevenueCat에 없는 이용권을 만료시키기 때문입니다.
- RevenueCat에 고객이 없으면(앱에서 구매 SDK를 한 번도 초기화하지 않은 회원) 404가 오므로, `POST /projects/{projectId}/customers`로 고객을 만든 뒤 한 번 다시 지급합니다.
- RevenueCat은 같은 이용권을 더 늦은 만료 시각으로 다시 지급하면 연장하지만, 기존 지급의 만료와 2시간 이내로 가까우면 중복으로 보고 무시합니다. 보상은 3일 단위로 이어 붙으므로 평소에는 문제가 되지 않습니다.
- 지급 도중 실패하면 502(`ENTITLEMENT_PROVIDER_UNAVAILABLE`)이고 수료는 `SUBMITTED`로 남습니다. 운영자가 같은 요청을 다시 보내면 그 시점 기준으로 다시 계산해 네 종류를 다시 지급합니다. 앞서 성공한 종류는 만료 시각이 몇 초만 달라 중복이 되는데, 이때 RevenueCat이 409를 돌려주면 `RevenueCatEntitlementGranter`는 이미 지급된 것으로 보고 다음 종류로 넘어갑니다. 중복 요청의 실제 응답 코드는 dev 프로젝트에서 확인해 이 문단을 갱신합니다.
- 승인은 수료 행 잠금 뒤 회원 ID advisory lock(`pg_advisory_xact_lock`)을 잡아 같은 회원의 다른 토픽 승인과 직렬화합니다. 이용권 동기화와 같은 키라 같은 트랜잭션 안에서 다시 잡아도 막히지 않습니다.

## 운영 런북

1. 제출이 들어오면 `SLACK_GRADUATION_WEBHOOK_URL`로 메시지가 옵니다(한국 시간). 전송 실패는 경고 로그만 남기고 제출은 그대로 성공합니다.
   ```
   [실습 인증 제출] 토픽 #7 분리불안 · userId 42 · graduationId 31 · 2번째 제출 · 2026-10-14T14:03
   영상(1시간): https://…/videos/graduation/42/….mp4?X-Amz-…
   키: videos/graduation/42/….mp4
   ```
2. 영상 링크를 눌러 봅니다. 1시간이 지났으면 S3 콘솔에서 키로 열거나 `aws s3 presign s3://<버킷>/<키>`로 다시 만듭니다.
3. 판정합니다. 메모는 앱에 그대로 보이므로 사용자에게 말하듯 적습니다(최대 500자). 보완 요청은 메모가 필수이고, 승인하면 같은 요청 안에서 이용권이 지급됩니다.
   ```bash
   curl -s -X PUT -H "X-Admin-Key: $ADMIN_API_KEY" -H "Content-Type: application/json" \
     -d '{"result":"APPROVED"}' https://<api>/api/admin/graduations/31/review
   curl -s -X PUT -H "X-Admin-Key: $ADMIN_API_KEY" -H "Content-Type: application/json" \
     -d '{"result":"REVISION_REQUESTED","note":"보호자가 현관 밖으로 나가는 장면이 보이지 않아요. 그 장면부터 다시 찍어 주세요."}' \
     https://<api>/api/admin/graduations/31/review
   ```
4. 판정 기준은 "실습을 시도한 장면이 확인되면 승인"입니다. 토픽과 무관한 영상도 별도 반려 없이 보완 요청으로 처리하고 메모에 적습니다.
5. 승인 응답이 502면 RevenueCat 지급에 실패한 것이고 수료는 검수 대기로 남아 있으므로 잠시 뒤 같은 요청을 다시 보냅니다.
6. 밀린 건은 SQL로 봅니다: `SELECT id, user_id, topic_id, attempt_no, submitted_at FROM topic_graduations WHERE status = 'SUBMITTED' ORDER BY submitted_at`.
7. 결과 알림(푸시)은 보내지 않습니다. 회원이 앱을 열면 보입니다.

## 필요 설정

| 변수 | 설명 |
|---|---|
| `ADMIN_API_KEY` | 운영자 API의 `X-Admin-Key`. 32자 이상 랜덤. 비어 있으면 `/api/admin/**` 전체가 404 ([security.md](security.md)) |
| `SLACK_GRADUATION_WEBHOOK_URL` | 제출 알림용 Slack incoming webhook. 비어 있으면 알림을 보내지 않음 |
| `REVENUECAT_API_KEY` | `customer_information:customers:read_write` 권한이 있는 v2 secret 키. 읽기 권한만 있으면 승인이 502로 실패 |

## 알려진 제약

- 재제출로 밀려난 영상 파일은 지우지 않습니다. `videos/graduation/` 수명주기 규칙으로 정리하며, 지워진 뒤 기록의 키를 열면 다운로드가 실패합니다.
- Slack 호출은 제출 트랜잭션 안에서 동기로 일어납니다(최악 읽기 타임아웃 3초). 트랜잭션이 롤백되면 알림만 나간 유령 제출이 생길 수 있습니다.
- RevenueCat 호출도 승인 트랜잭션 안에서 일어납니다. 네 종류 지급과 동기화가 모두 끝난 뒤에만 승인이 커밋됩니다.
- 운영자용 목록 API가 없습니다. 대기 건은 Slack 알림과 SQL로 봅니다.
