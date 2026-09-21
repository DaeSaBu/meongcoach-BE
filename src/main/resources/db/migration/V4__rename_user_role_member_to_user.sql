-- 역할 명칭을 엔티티명(User)에 맞춰 MEMBER → USER, ONBOARDING_MEMBER → ONBOARDING_USER로 통일한다.
-- role은 CHECK 제약 없는 VARCHAR(20)이라 값만 갱신한다.
UPDATE "users" SET "role" = 'USER' WHERE "role" = 'MEMBER';
UPDATE "users" SET "role" = 'ONBOARDING_USER' WHERE "role" = 'ONBOARDING_MEMBER';
