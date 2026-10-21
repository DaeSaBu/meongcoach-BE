ALTER TABLE "graduation_submissions"
    ADD COLUMN "status" VARCHAR(30) NOT NULL,
    ADD COLUMN "reviewed_at" TIMESTAMPTZ,
    ADD COLUMN "review_note" VARCHAR(500),
    ADD COLUMN "retake_image_url" VARCHAR(512),
    ADD COLUMN "retake_instructions" JSONB NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE "graduation_submissions" ADD CONSTRAINT "chk_graduation_submissions_retake_instructions_array"
    CHECK (jsonb_typeof("retake_instructions") = 'array');

ALTER TABLE "topic_graduations" ADD COLUMN "current_submission_id" BIGINT UNIQUE
    REFERENCES "graduation_submissions" ("id");

ALTER TABLE "topic_graduations"
    DROP COLUMN "reviewed_at",
    DROP COLUMN "review_note",
    DROP COLUMN "retake_image_url",
    DROP COLUMN "retake_instructions";
