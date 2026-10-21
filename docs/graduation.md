# 실습 인증 제출과 판정 연동

회원·토픽별 `graduation_submissions.id`가 가장 큰 제출로 현재 상태를 판단한다. `topic_graduations`는 V22에서 제거한다. 제출이 없으면 첫 제출, `REVISION_REQUESTED`이면 새 제출을 생성한다. `SUBMITTED`와 `APPROVED`에서는 URL 발급과 추가 제출을 거절한다. 이전 제출의 영상·판정은 보존한다.

## API 계약

기존 `/api/training/topics/{topicId}/graduation`과 하위 `presigned-urls`, `submissions`, 목록 `/api/training/graduations` 경로를 유지한다. 상세의 `graduation` 래퍼도 유지한다.

- 상세·목록·제출 응답의 `graduationId`는 `submissionId`로 변경한다. `attemptNo`는 없다.
- 상세·목록의 `review`는 `{"note": null, "retakeInstructions": []}` 형태다. 재촬영 지시는 배열 순서를 유지한다.
- `reviewedAt`은 판정 시각이며, 승인 시각도 승인된 제출의 `reviewedAt`이다.
- 상세의 `retakeImageUrl`은 `review` 밖에 남는다. 없으면 null이다.
- 검수 대기는 409 `GRADUATION_REVIEW_PENDING`, 승인은 409 `GRADUATION_ALREADY_APPROVED`다.
- 동시 최초 제출·보완 후 재제출의 DB 충돌은 409 `GRADUATION_SUBMISSION_CONFLICT`다. 앱은 409 후 최신 상태를 재조회한다.

목록은 토픽별 최대 제출 ID를 선택한 뒤 `submitted_at DESC, id DESC`로 정렬한다. 상세는 회원·토픽별 최대 ID 한 건이다.

## 관리자 도구 전환

외부 관리자 도구 코드는 이 변경에 포함하지 않는다. 관리자 조회·판정 SQL은 아래 계약으로 전환해야 한다.

1. 회원과 토픽은 제출의 `user_id`, `topic_id`에서 읽는다. 부모 조인은 제거한다.
2. 검수 대상은 회원·토픽별 최대 ID이면서 `status = 'SUBMITTED'`인 제출이다.
3. 메모와 지시는 `review` JSONB에 함께 저장한다. 보완 요청의 `note`는 공백이 아닌 문자열이어야 한다. 빈 메모의 승인은 JSON null을 저장한다.
4. 승인·보완 요청은 화면에서 읽은 제출 ID와 버전을 조건으로 갱신한다. `version`만 증가시키고 이전 버전을 조건에 넣지 않으면 동시 판정을 막지 못한다.
5. 영향받은 행 수가 0이면 충돌로 처리하고 다시 조회한다. 새 버전을 임의로 읽어 같은 판정을 자동 재시도하지 않는다.

```sql
UPDATE graduation_submissions s
SET status = :decision,
    reviewed_at = :reviewed_at,
    review = CAST(:review_json AS jsonb),
    retake_image_url = :retake_image_url,
    version = version + 1
WHERE s.id = :submission_id
  AND s.version = :expected_version
  AND s.status = 'SUBMITTED'
  AND s.id = (
      SELECT max(latest.id) FROM graduation_submissions latest
      WHERE latest.user_id = s.user_id AND latest.topic_id = s.topic_id
  );
```

`:decision`은 `APPROVED` 또는 `REVISION_REQUESTED`만 허용한다. 보완 요청의 필수 메모 검증은 관리자 입력에서도 수행한다. 판정 후 영상과 제출 시각은 바꾸지 않는다.

## V22 이관과 적용 순서

V22는 V21 스키마의 제출에 회원·토픽·버전을 추가하고 기존 메모·지시를 JSON으로 옮긴다. 제출이 없는 부모는 새 제출로 옮기며, 부모의 상태·영상·제출 시각·승인 시각을 보존한다. 이 부모에는 V21 기준 판정 메모 컬럼이 없으므로 빈 `review`를 사용한다. 기존 마이그레이션은 수정하지 않는다.

`SUBMITTED` 또는 `APPROVED`인 행에만 `(user_id, topic_id)` 부분 유니크 인덱스를 적용한다. 같은 회원·토픽에 이 상태가 둘 이상이면 이관은 실패하고 트랜잭션이 롤백된다. 데이터를 임의 삭제하거나 판정을 추정하지 않는다. 다음 조회로 충돌을 확인하고 운영자가 원본을 확인해야 한다.

```sql
SELECT g.user_id, g.topic_id, count(*)
FROM graduation_submissions s
JOIN topic_graduations g ON g.id = s.graduation_id
WHERE s.status IN ('SUBMITTED', 'APPROVED')
GROUP BY g.user_id, g.topic_id
HAVING count(*) > 1;
```

백엔드·앱·관리자 도구는 함께 전환한다. 부모 테이블을 사용하는 구 서버와 관리자 쓰기를 먼저 중지한 뒤 V22와 새 서버를 적용한다. 새 계약의 앱을 적용하고 관리자 판정을 재개한다. 구 서버가 남아 있는 롤링 배포와 구 앱의 호환성은 보장하지 않는다.

dev API가 새 계약으로 준비된 뒤 Android·iOS에서 확인창 취소 시 영상 유지·요청 없음, 확인 연타 시 한 번 제출, 검수 대기 진입 제한, 보완 후 재제출, 409 후 재조회를 확인한다. 이번 로컬 검증은 백엔드 테스트·패키징과 앱 단위 테스트·번들 빌드까지 수행하며 실제 기기 제출 검증은 배포 후 남아 있다.
