ALTER TABLE "graduation_submissions"
    DROP CONSTRAINT "uk_graduation_submissions_graduation_id_attempt_no",
    DROP COLUMN "attempt_no";

ALTER TABLE "topic_graduations" DROP COLUMN "attempt_no";

CREATE INDEX "idx_graduation_submissions_graduation_id_id"
    ON "graduation_submissions" ("graduation_id", "id" DESC);
