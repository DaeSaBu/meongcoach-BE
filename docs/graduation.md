# 토픽 수료(실습 인증)

토픽의 모든 커리큘럼을 마친 회원이 대표 실습 영상을 올리면 운영자가 판정합니다. 승인하면 토픽 수료로 표시되고, 보상(이용권 지급)은 없습니다.
화면 문구는 `실습 인증`·`토픽 수료`이고, 코드·API·테이블 이름은 `graduation`입니다. 기획 원문은 기획 저장소의 `practice-certification-plan.md`입니다.

## 전체 흐름

```
앱: POST /api/training/topics/{topicId}/graduation/presigned-urls  → presigned PUT URL (DB 변경 없음)
앱: S3에 영상 PUT (videos/graduation/{userId}/{uuid}.mp4)
앱: POST /api/training/topics/{topicId}/graduation/submissions      → 수료 행 생성·갱신 + 기록 1줄
운영자: 로컬 관리자 페이지에서 대기 목록·영상 확인                  → 승인 또는 보완 요청을 DB에 직접 적용
앱: GET /api/training/topics/{topicId}/graduation                    → 상태 확인(화면에 들어올 때마다 재조회)
앱: GET /api/training/graduations                                    → 마이페이지 "내 훈련 영상" 목록(제출한 토픽만, 최근 제출 순)
```

업로드 완료를 S3 이벤트로 감지하지 않습니다. 앱이 업로드를 마친 뒤 제출합니다. 같은 버킷의 S3→SQS 이벤트는
AI 분석 소비자(`VideoUploadSqsConsumer`)가 `videos/training/` 접두사만 처리하므로 실습 영상은 무시됩니다.

## 데이터

| 테이블 | 단위 | 내용 |
|---|---|---|
| `topic_graduations` | 회원·토픽당 1행(`uk_topic_graduations_user_id_topic_id`) | 지금 상태(`SUBMITTED`·`REVISION_REQUESTED`·`APPROVED`), 가장 최근 제출의 차수·영상 키·제출 시각, 판정 시각·운영자 메모, 승인 시각 |
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
- 판정은 서버 API가 없습니다. 아래 로컬 관리자 페이지가 `SUBMITTED`인 행에만 적용합니다. 엔티티의 `approve`·`requestRevision`은 같은 규칙을 코드로 남겨 둔 것이며 테스트 준비에만 씁니다.

## 검수: 로컬 관리자 페이지

아직 임시 기능이라 운영자 API를 배포하지 않습니다. 운영자는 기획 저장소의 `practice-certification/admin-local/`
(배포하지 않는 Node 페이지, 사용법은 그 폴더의 README)를 자기 컴퓨터에서 띄우고, bastion 터널로 연 dev·prod DB에 직접 붙어 판정합니다.
관리자 역할·관리자 키·Slack 알림은 없습니다.

- 대기 목록: `status = 'SUBMITTED'`인 `topic_graduations`를 제출 시각 역순으로 보여 줍니다.
- 영상: `video_object_key`로 1시간짜리 presigned GET URL을 만들어 재생합니다(`S3VideoStorage.issueDownloadUrl`과 같은 방식).
- 승인: `status = 'APPROVED'`, `reviewed_at = approved_at = now()`, `review_note`는 비어 있으면 null.
- 보완 요청: 메모 필수(최대 500자), `status = 'REVISION_REQUESTED'`, `reviewed_at = now()`.
- 두 판정 모두 `WHERE id = ? AND status = 'SUBMITTED'`로만 바꿉니다. 그사이 회원이 다시 제출해도 행은 `SUBMITTED`로 남으므로 새 영상을 보고 판정하면 됩니다.
- `graduation_submissions`는 건드리지 않습니다.
- 메모는 앱에 그대로 보이므로 사용자에게 말하듯 적습니다. 판정 기준은 "실습을 시도한 장면이 확인되면 승인"이고, 토픽과 무관한 영상도 보완 요청으로 처리합니다.
- 결과 알림(푸시)은 보내지 않습니다. 회원이 앱을 열면 보입니다.

## 알려진 제약

- 재제출로 밀려난 영상 파일은 지우지 않습니다. `videos/graduation/` 수명주기 규칙으로 정리하며, 지워진 뒤 기록의 키를 열면 다운로드가 실패합니다.
- 제출 알림이 없습니다. 운영자가 로컬 관리자 페이지를 열어 대기 건을 확인합니다.
- 판정은 서버를 거치지 않으므로 서버 로그·감사 기록이 남지 않습니다. 판정 시각과 메모는 `topic_graduations` 행에만 남습니다.
