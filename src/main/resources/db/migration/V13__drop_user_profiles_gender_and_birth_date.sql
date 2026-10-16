-- 사용자 프로필의 성별·생년월일 컬럼을 지운다. 기존 값은 보존하지 않는다.
ALTER TABLE "user_profiles" DROP COLUMN "gender";
ALTER TABLE "user_profiles" DROP COLUMN "birth_date";
