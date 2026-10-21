ALTER TABLE graduation_submissions
    ADD COLUMN user_id BIGINT,
    ADD COLUMN topic_id BIGINT,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN review JSONB;

UPDATE graduation_submissions s
SET user_id = g.user_id,
    topic_id = g.topic_id,
    review = jsonb_build_object('note', s.review_note, 'retakeInstructions', s.retake_instructions)
FROM topic_graduations g
WHERE s.graduation_id = g.id;

INSERT INTO graduation_submissions
    (graduation_id, user_id, topic_id, status, video_object_key, submitted_at, reviewed_at, created_at, review)
SELECT g.id, g.user_id, g.topic_id, g.status, g.video_object_key, g.submitted_at, g.approved_at, g.created_at,
       '{"note": null, "retakeInstructions": []}'::jsonb
FROM topic_graduations g
WHERE NOT EXISTS (SELECT 1 FROM graduation_submissions s WHERE s.graduation_id = g.id);

ALTER TABLE graduation_submissions
    ALTER COLUMN user_id SET NOT NULL,
    ALTER COLUMN topic_id SET NOT NULL,
    ALTER COLUMN review SET NOT NULL,
    ALTER COLUMN review SET DEFAULT '{"note": null, "retakeInstructions": []}'::jsonb,
    ADD CONSTRAINT chk_graduation_submissions_review CHECK (
        jsonb_typeof(review) = 'object'
        AND review ? 'note' AND review ? 'retakeInstructions'
        AND jsonb_typeof(review -> 'note') IN ('string', 'null')
        AND jsonb_typeof(review -> 'retakeInstructions') = 'array'
    );

CREATE UNIQUE INDEX uk_graduation_submissions_active
    ON graduation_submissions (user_id, topic_id)
    WHERE status IN ('SUBMITTED', 'APPROVED');
CREATE INDEX idx_graduation_submissions_user_topic_id
    ON graduation_submissions (user_id, topic_id, id DESC);

ALTER TABLE graduation_submissions
    DROP COLUMN graduation_id,
    DROP COLUMN review_note,
    DROP COLUMN retake_instructions;
DROP TABLE topic_graduations;
