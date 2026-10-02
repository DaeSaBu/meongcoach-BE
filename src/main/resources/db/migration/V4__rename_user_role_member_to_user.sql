-- 역할 명칭을 엔티티명(User)에 맞춰 MEMBER → USER, ONBOARDING_MEMBER → ONBOARDING_USER로 통일한다.
-- dev/prod DB는 Flyway 도입 전 ddl-auto가 만든 스키마를 baseline한 것이라 Hibernate가 생성한 users_role_check가 남아 있다.
-- V1 기준 스키마에는 없는 제약이고 옛 값만 허용하므로 제거한 뒤 값을 갱신한다. 빈 DB에서는 IF EXISTS로 건너뛴다.
ALTER TABLE "users" DROP CONSTRAINT IF EXISTS "users_role_check";
UPDATE "users" SET "role" = 'USER' WHERE "role" = 'MEMBER';
UPDATE "users" SET "role" = 'ONBOARDING_USER' WHERE "role" = 'ONBOARDING_MEMBER';
