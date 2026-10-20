-- 과거 차수는 판정 정보가 없어 추정하지 않고 미상으로 남긴다.
ALTER TABLE "graduation_submissions"
    ADD COLUMN "status" VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN "reviewed_at" TIMESTAMPTZ,
    ADD COLUMN "review_note" VARCHAR(500),
    ADD COLUMN "retake_framing_icon_code" VARCHAR(30),
    ADD COLUMN "retake_instructions" JSONB NOT NULL DEFAULT '[]'::jsonb;

-- 현재 차수의 기록이 없는 기존 데이터도 최신 영상 정보로 복원한다.
INSERT INTO "graduation_submissions" ("graduation_id", "attempt_no", "video_object_key", "submitted_at", "created_at")
SELECT "id", "attempt_no", "video_object_key", "submitted_at", "created_at"
FROM "topic_graduations"
ON CONFLICT ("graduation_id", "attempt_no") DO NOTHING;

UPDATE "graduation_submissions" s
SET "status" = g."status",
    "reviewed_at" = g."reviewed_at",
    "review_note" = g."review_note",
    "retake_framing_icon_code" = g."retake_framing_icon_code",
    "retake_instructions" = g."retake_instructions"
FROM "topic_graduations" g
WHERE s."graduation_id" = g."id" AND s."attempt_no" = g."attempt_no";

ALTER TABLE "graduation_submissions" ALTER COLUMN "status" DROP DEFAULT;
ALTER TABLE "graduation_submissions" ADD CONSTRAINT "chk_graduation_submissions_retake_instructions_array"
    CHECK (jsonb_typeof("retake_instructions") = 'array');

ALTER TABLE "topic_graduations" ADD COLUMN "current_submission_id" BIGINT UNIQUE
    REFERENCES "graduation_submissions" ("id");
UPDATE "topic_graduations" g
SET "current_submission_id" = s."id"
FROM "graduation_submissions" s
WHERE s."graduation_id" = g."id" AND s."attempt_no" = g."attempt_no";

ALTER TABLE "topic_graduations"
    DROP COLUMN "reviewed_at",
    DROP COLUMN "review_note",
    DROP COLUMN "retake_framing_icon_code",
    DROP COLUMN "retake_instructions";
