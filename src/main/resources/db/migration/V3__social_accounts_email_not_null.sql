-- 소셜 계정 이메일 필수화(Email 값 객체 전역 NOT NULL). email이 null인 기존 행이 있으면 이 마이그레이션은 실패하므로
-- 적용 전에 해당 행을 직접 정리한다.
ALTER TABLE "social_accounts" ALTER COLUMN "email" SET NOT NULL;
