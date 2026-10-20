-- 보완 요청 때 운영자가 고른 재촬영 안내(graduation/domain/TopicGraduation). 로컬 관리자 페이지가 판정과 함께 쓰고, 서버는 앱에 전달만 한다.
-- 촬영 구도는 앱이 가진 그림 코드다. 그림을 늘릴 때 서버 배포가 필요 없도록 코드 목록을 DB 제약으로 묶지 않는다.
ALTER TABLE "topic_graduations" ADD COLUMN "retake_framing" VARCHAR(30);

-- 재촬영 팁. 보완 요청 1건에 최대 3개를 sort_order 0부터 순서대로 둔다. 다시 제출하면 지운다.
CREATE TABLE "graduation_retake_tips" (
    "graduation_id" BIGINT NOT NULL REFERENCES "topic_graduations" ("id"),
    "sort_order" INTEGER NOT NULL,
    "tip" VARCHAR(60) NOT NULL,
    PRIMARY KEY ("graduation_id", "sort_order")
);
