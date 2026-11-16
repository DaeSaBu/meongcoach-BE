ALTER TABLE "ai_analyses" ADD COLUMN "dog_id" BIGINT;

UPDATE "ai_analyses" a
SET "dog_id" = COALESCE(
    (SELECT MIN(d."id") FROM "dogs" d
     WHERE d."user_id" = a."user_id" AND d."status" = 'SELECTED' AND d."deleted_at" IS NULL),
    (SELECT MIN(d."id") FROM "dogs" d WHERE d."user_id" = a."user_id"));

WITH removed AS (
    DELETE FROM "ai_analyses" WHERE "dog_id" IS NULL RETURNING "report_id"
)
DELETE FROM "ai_reports" WHERE "id" IN (SELECT "report_id" FROM removed WHERE "report_id" IS NOT NULL);

ALTER TABLE "ai_analyses" ALTER COLUMN "dog_id" SET NOT NULL;

CREATE INDEX "idx_ai_analyses_user_dog_created_at"
    ON "ai_analyses" ("user_id", "dog_id", "created_at" DESC, "id" DESC);
