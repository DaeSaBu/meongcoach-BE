-- 기존 안내를 순서대로 옮긴 뒤 별도 테이블을 제거한다.
ALTER TABLE "topic_graduations" RENAME COLUMN "retake_framing" TO "retake_framing_icon_code";
ALTER TABLE "topic_graduations" ADD COLUMN "retake_instructions" JSONB NOT NULL DEFAULT '[]'::jsonb;

UPDATE "topic_graduations" g
SET "retake_instructions" = tips.instructions
FROM (
    SELECT "graduation_id", jsonb_agg("tip" ORDER BY "sort_order") AS instructions
    FROM "graduation_retake_tips"
    GROUP BY "graduation_id"
) tips
WHERE g."id" = tips."graduation_id";

DROP TABLE "graduation_retake_tips";
ALTER TABLE "topic_graduations" ADD CONSTRAINT "chk_topic_graduations_retake_instructions_array"
    CHECK (jsonb_typeof("retake_instructions") = 'array');
