-- 이전 제출에는 판정이 저장되지 않아 여러 이력의 상태를 임의로 복원하지 않는다.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM graduation_submissions GROUP BY graduation_id HAVING count(*) > 1) THEN
        RAISE EXCEPTION '이전 제출의 판정 이력을 확인한 뒤 단일 제출 구조로 이관해야 합니다.';
    END IF;
END $$;

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
WHERE s.graduation_id = g.id;

INSERT INTO graduation_submissions
    (graduation_id, attempt_no, user_id, topic_id, version, status, video_object_key,
     submitted_at, reviewed_at, retake_image_url, created_at, review)
SELECT g.id, g.attempt_no, g.user_id, g.topic_id, g.version, g.status, g.video_object_key,
       g.submitted_at, COALESCE(g.reviewed_at, g.approved_at), g.retake_image_url, g.created_at,
       jsonb_build_object('note', g.review_note, 'retakeInstructions', g.retake_instructions)
FROM topic_graduations g
WHERE NOT EXISTS (SELECT 1 FROM graduation_submissions s WHERE s.graduation_id = g.id);

ALTER TABLE graduation_submissions
    ALTER COLUMN video_object_key DROP NOT NULL,
    ALTER COLUMN submitted_at DROP NOT NULL,
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

CREATE UNIQUE INDEX uk_graduation_submissions_initial
    ON graduation_submissions (user_id, topic_id)
    WHERE status = 'NOT_SUBMITTED';

INSERT INTO graduation_submissions (user_id, topic_id, status, created_at)
SELECT DISTINCT p.user_id, c.topic_id, 'NOT_SUBMITTED', now()
FROM lesson_progress p
JOIN lessons l ON l.id = p.lesson_id
JOIN curriculums c ON c.id = l.curriculum_id
WHERE p.completed_count > 0
  AND NOT EXISTS (
    SELECT 1 FROM graduation_submissions s WHERE s.user_id = p.user_id AND s.topic_id = c.topic_id
  )
  AND NOT EXISTS (
    SELECT 1 FROM curriculums curriculum
    WHERE curriculum.topic_id = c.topic_id
      AND (
        NOT EXISTS (SELECT 1 FROM lessons lesson WHERE lesson.curriculum_id = curriculum.id)
        OR EXISTS (
          SELECT 1 FROM lessons lesson
          WHERE lesson.curriculum_id = curriculum.id
            AND NOT EXISTS (
              SELECT 1 FROM lesson_progress progress
              WHERE progress.user_id = p.user_id AND progress.lesson_id = lesson.id AND progress.completed_count > 0
            )
        )
      )
  );
