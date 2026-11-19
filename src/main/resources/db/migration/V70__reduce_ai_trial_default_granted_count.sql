-- 기본 부여 횟수를 30회에서 3회로 줄인다. 쿠폰 등으로 추가 부여한 횟수는 그대로 둔다.
-- 사용 횟수는 기본 부여분부터 쓴 것으로 보고, 기본 부여분에서 쓴 횟수는 3회까지만 남긴다.
UPDATE "ai_trials"
SET "used_count" = LEAST(
        LEAST("used_count", 3) + GREATEST("used_count" - 30, 0),
        GREATEST("granted_count" - 27, 0)
    ),
    "granted_count" = GREATEST("granted_count" - 27, 0);

ALTER TABLE "ai_trials" ALTER COLUMN "granted_count" SET DEFAULT 3;
