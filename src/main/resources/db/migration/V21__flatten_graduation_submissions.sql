-- 보완 요청 뒤 재제출할 때만 차수가 올랐으므로 최신 차수보다 낮은 차수는 모두 보완 요청을 받은 제출이다.
-- 그 판정 메모·시각은 재제출 때 덮어써져 복원할 수 없으므로 빈 판정으로 남기고, 최신 차수만 부모 상태를 이어받는다.
ALTER TABLE graduation_submissions
    ADD COLUMN user_id BIGINT,
    ADD COLUMN topic_id BIGINT,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN status VARCHAR(30),
    ADD COLUMN reviewed_at TIMESTAMPTZ,
    ADD COLUMN retake_image_url VARCHAR(512),
    ADD COLUMN review JSONB;

UPDATE graduation_submissions s
SET user_id = g.user_id,
    topic_id = g.topic_id,
    version = g.version,
    status = g.status,
    reviewed_at = COALESCE(g.reviewed_at, g.approved_at),
    retake_image_url = g.retake_image_url,
    review = jsonb_build_object('note', g.review_note, 'retakeInstructions', g.retake_instructions)
FROM topic_graduations g
WHERE s.graduation_id = g.id
  AND s.attempt_no = g.attempt_no;

UPDATE graduation_submissions s
SET user_id = g.user_id,
    topic_id = g.topic_id,
    version = 0,
    status = 'REVISION_REQUESTED',
    reviewed_at = NULL,
    retake_image_url = NULL,
    review = '{"note": null, "retakeInstructions": []}'::jsonb
FROM topic_graduations g
WHERE s.graduation_id = g.id
  AND s.attempt_no < g.attempt_no;

INSERT INTO graduation_submissions
    (graduation_id, attempt_no, user_id, topic_id, version, status, video_object_key,
     submitted_at, reviewed_at, retake_image_url, created_at, review)
SELECT g.id, g.attempt_no, g.user_id, g.topic_id, g.version, g.status, g.video_object_key,
       g.submitted_at, COALESCE(g.reviewed_at, g.approved_at), g.retake_image_url, g.created_at,
       jsonb_build_object('note', g.review_note, 'retakeInstructions', g.retake_instructions)
FROM topic_graduations g
WHERE NOT EXISTS (
    SELECT 1 FROM graduation_submissions s WHERE s.graduation_id = g.id AND s.attempt_no = g.attempt_no
);

ALTER TABLE graduation_submissions
    ALTER COLUMN user_id SET NOT NULL,
    ALTER COLUMN topic_id SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN review SET NOT NULL,
    ALTER COLUMN review SET DEFAULT '{"note": null, "retakeInstructions": []}'::jsonb,
    ADD CONSTRAINT chk_graduation_submissions_review CHECK (
        jsonb_typeof(review) = 'object'
        AND review ? 'note' AND review ? 'retakeInstructions'
        AND jsonb_typeof(review -> 'note') IN ('string', 'null')
        AND jsonb_typeof(review -> 'retakeInstructions') = 'array'
    ),
    DROP COLUMN graduation_id,
    DROP COLUMN attempt_no;

CREATE UNIQUE INDEX uk_graduation_submissions_active
    ON graduation_submissions (user_id, topic_id)
    WHERE status IN ('SUBMITTED', 'APPROVED');
CREATE INDEX idx_graduation_submissions_user_topic_id
    ON graduation_submissions (user_id, topic_id, id DESC);

DROP TABLE topic_graduations;
