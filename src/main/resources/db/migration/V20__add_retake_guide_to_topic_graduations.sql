-- 보완 요청 때 운영자가 남긴 재촬영 지시와 직접 올린 촬영 예시 이미지(R2 공개 URL)를 저장한다.
ALTER TABLE "topic_graduations"
    ADD COLUMN "retake_image_url" VARCHAR(512),
    ADD COLUMN "retake_instructions" JSONB NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE "topic_graduations" ADD CONSTRAINT "chk_topic_graduations_retake_instructions_array"
    CHECK (jsonb_typeof("retake_instructions") = 'array');
