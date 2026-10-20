# 실습 인증의 제출과 판정

## 저장 구조

`topic_graduations`는 사용자와 토픽 조합당 한 행이며 수료 진행 상태와 현재 제출을 관리한다.
`graduation_submissions`는 차수별 영상과 해당 영상에 대한 판정을 보관한다.

| 데이터 | 저장 위치 |
|---|---|
| 사용자·토픽·현재 상태·차수·현재 영상·제출 시각 | `topic_graduations` |
| 현재 제출 참조 | `topic_graduations.current_submission_id` |
| 최종 수료 시각 | `topic_graduations.approved_at` |
| 차수별 영상·제출 시각 | `graduation_submissions` |
| 차수별 판정 상태·메모·판정 시각 | `graduation_submissions` |
| 차수별 재촬영 지시·구도 아이콘 코드 | `graduation_submissions` |

검수 대기 중 영상 교체는 같은 차수의 영상과 제출 시각을 갱신한다.
보완 요청 후 재제출은 다음 차수의 제출을 만들고 현재 제출 참조를 변경한다.
이전 차수의 판정과 재촬영 안내는 지우지 않는다.
새 차수는 `SUBMITTED` 상태이며 판정 시각·메모·구도 코드는 null, 재촬영 지시는 빈 배열이다.

API 응답의 `reviewedAt`, `reviewNote`, `retakeInstructions`, `retakeFramingIconCode`는 현재 제출에서 읽는다.
기존 응답 형식을 유지하며 과거 판정을 조회하는 API는 추가하지 않는다.
상세·목록 조회는 현재 제출을 fetch join하여 판정 조회가 건수만큼 반복되지 않게 한다.

## 마이그레이션

V20은 V19의 현재 판정과 안내를 같은 차수의 Submission으로 이전하고 현재 제출 참조를 연결한다.
현재 차수의 Submission이 없는 행은 현재 영상·제출 시각으로 복원한다.
이전 차수는 판정 정보가 남아 있지 않으므로 상태를 `UNKNOWN`으로 설정한다.
이미 삭제된 판정 메모·안내는 복구할 수 없으며 과거 차수를 임의로 보완 요청 처리하지 않는다.

`current_submission_id`는 최초 TopicGraduation 저장 후 Submission을 생성·연결하는 동안 null일 수 있다.
제출 서비스는 같은 트랜잭션 안에서 연결을 완료한다.
외래 키가 양방향으로 존재하므로 수료 데이터를 수동 삭제할 때는 현재 제출 참조를 먼저 비워야 한다.

V20은 기존 판정 컬럼을 제거하므로 구 버전 서버·관리자 페이지와 동시에 운영하지 않는다.
서버와 관리자 페이지의 DB 접근을 함께 전환하고, 맞춤 플랜 등 다른 PR과 마이그레이션 번호를 확인한다.

## 운영자 판정

판정 쓰기 API는 없다. DB를 직접 수정하는 관리자 페이지도 아래 저장 규칙을 따라야 한다.
현재 관리자 페이지의 실제 코드는 이 백엔드 저장소 밖에 있으며 별도 변경이 필요하다.

- 목록 조회 시 `current_submission_id`로 Submission을 연결해 판정과 안내를 읽는다.
- 판정할 때 조회한 수료 ID와 버전을 전달한다.
- 수료 행을 잠근 뒤 현재 Submission과 수료 상태를 같은 트랜잭션에서 갱신한다.
- 수료의 `version`을 증가시켜 이전 버전의 영상 교체가 판정을 덮어쓰지 못하게 한다.
- 갱신 건수가 0이면 최신 상태를 다시 조회한다. 기존 화면으로 판정을 강행하지 않는다.
- 재촬영 지시는 최대 3개, 각 60자 이내의 문자열 JSON 배열로 전달한다.
- 현재 Submission은 서버가 제출할 때 생성한다. 운영자가 과거 차수나 판정 완료 차수를 수정하지 않는다.

다음은 이름 있는 바인딩 파라미터를 사용하는 판정 SQL 예시다.
`:version`은 판정 화면을 조회했을 때 읽은 `topic_graduations.version`이다.

### 보완 요청

```sql
WITH locked AS (
    SELECT id, current_submission_id
    FROM topic_graduations
    WHERE id = :graduation_id AND version = :version AND status = 'SUBMITTED'
    FOR UPDATE
), reviewed AS (
    UPDATE graduation_submissions s
    SET status = 'REVISION_REQUESTED',
        reviewed_at = now(),
        review_note = :review_note,
        retake_instructions = CAST(:retake_instructions AS jsonb),
        retake_framing_icon_code = :retake_framing_icon_code
    FROM locked g
    WHERE s.id = g.current_submission_id
      AND s.graduation_id = g.id AND s.status = 'SUBMITTED'
    RETURNING s.graduation_id
)
UPDATE topic_graduations g
SET status = 'REVISION_REQUESTED', version = version + 1, updated_at = now()
FROM reviewed r
WHERE g.id = r.graduation_id;
```

### 승인

```sql
WITH locked AS (
    SELECT id, current_submission_id
    FROM topic_graduations
    WHERE id = :graduation_id AND version = :version AND status = 'SUBMITTED'
    FOR UPDATE
), reviewed AS (
    UPDATE graduation_submissions s
    SET status = 'APPROVED', reviewed_at = now(), review_note = :review_note,
        retake_instructions = '[]'::jsonb, retake_framing_icon_code = NULL
    FROM locked g
    WHERE s.id = g.current_submission_id
      AND s.graduation_id = g.id AND s.status = 'SUBMITTED'
    RETURNING s.graduation_id
)
UPDATE topic_graduations g
SET status = 'APPROVED', approved_at = now(), version = version + 1, updated_at = now()
FROM reviewed r
WHERE g.id = r.graduation_id;
```

메모는 앞뒤 공백을 제거하고 빈 문자열이면 null로 변환한다. 보완 요청 메모는 비어 있으면 안 된다.
