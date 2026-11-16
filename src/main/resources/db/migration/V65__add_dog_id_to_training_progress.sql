ALTER TABLE "lesson_progress" ADD COLUMN "dog_id" BIGINT;
ALTER TABLE "topic_selection" ADD COLUMN "dog_id" BIGINT;

UPDATE "lesson_progress" p
SET "dog_id" = COALESCE(
    (SELECT MIN(d."id") FROM "dogs" d
     WHERE d."user_id" = p."user_id" AND d."status" = 'SELECTED' AND d."deleted_at" IS NULL),
    (SELECT MIN(d."id") FROM "dogs" d WHERE d."user_id" = p."user_id"));

UPDATE "topic_selection" s
SET "dog_id" = COALESCE(
    (SELECT MIN(d."id") FROM "dogs" d
     WHERE d."user_id" = s."user_id" AND d."status" = 'SELECTED' AND d."deleted_at" IS NULL),
    (SELECT MIN(d."id") FROM "dogs" d WHERE d."user_id" = s."user_id"));

DELETE FROM "lesson_progress" WHERE "dog_id" IS NULL;
DELETE FROM "topic_selection" WHERE "dog_id" IS NULL;

-- dev/prod DB는 Flyway 도입 전 ddl-auto가 만든 스키마를 baseline한 것이라 기존 유니크 제약의 이름이 V1과 다르다.
-- 두 테이블에는 유니크 제약이 하나씩만 있으므로 이름 대신 종류로 찾아 지운다.
DO $$
DECLARE
    target RECORD;
BEGIN
    FOR target IN
        SELECT c.conrelid::regclass AS table_name, c.conname AS constraint_name
        FROM pg_constraint c
        WHERE c.conrelid IN ('lesson_progress'::regclass, 'topic_selection'::regclass)
          AND c.contype = 'u'
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', target.table_name, target.constraint_name);
    END LOOP;
END $$;

ALTER TABLE "lesson_progress"
    ALTER COLUMN "dog_id" SET NOT NULL,
    ADD CONSTRAINT "uk_lesson_progress_user_dog_lesson" UNIQUE ("user_id", "dog_id", "lesson_id");

ALTER TABLE "topic_selection"
    ALTER COLUMN "dog_id" SET NOT NULL,
    ADD CONSTRAINT "uk_topic_selection_user_dog" UNIQUE ("user_id", "dog_id");
