-- 이용권의 활성 여부를 만료 시각 하나로 나타낸다. NULL이면 무기한, 지난 시각이면 비활성이다.
ALTER TABLE "entitlements" ADD COLUMN "expires_at" TIMESTAMPTZ;

-- 회수된 이용권은 회수 시각을 만료 시각으로 옮겨 비활성으로 남기고, 활성 이용권은 무기한으로 둔다
UPDATE "entitlements" SET "expires_at" = "revoked_at" WHERE "revoked_at" IS NOT NULL;

ALTER TABLE "entitlements" DROP COLUMN "revoked_at";
