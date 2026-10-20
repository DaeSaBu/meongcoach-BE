-- 보완 요청 때 운영자가 남긴 재촬영 지시와 앱의 구도 아이콘 코드를 저장한다.
ALTER TABLE "topic_graduations"
    ADD COLUMN "retake_framing_icon_code" VARCHAR(30),
    ADD COLUMN "retake_instructions" JSONB NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE "topic_graduations" ADD CONSTRAINT "chk_topic_graduations_retake_instructions_array"
    CHECK (jsonb_typeof("retake_instructions") = 'array');
