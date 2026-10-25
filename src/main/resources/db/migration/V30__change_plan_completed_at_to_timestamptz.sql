-- V28이 dev에 적용된 뒤 파일이 수정돼 체크섬이 어긋났으므로, V28은 적용된 내용으로 되돌리고 타입 변경을 별도 버전으로 적용한다.
-- 완료 시각은 Instant로 다루므로 시간대 정보가 있는 TIMESTAMPTZ로 통일한다.
ALTER TABLE plans
    ALTER COLUMN completed_at TYPE TIMESTAMPTZ USING completed_at AT TIME ZONE 'UTC';
