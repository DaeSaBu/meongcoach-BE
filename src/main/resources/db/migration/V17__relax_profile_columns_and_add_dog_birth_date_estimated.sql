-- 보호자 MBTI와 강아지 몸무게를 선택 입력으로 바꾼다. NULL은 미입력이다
ALTER TABLE "user_profiles" ALTER COLUMN "mbti" DROP NOT NULL;
ALTER TABLE "dogs" ALTER COLUMN "weight_kg" DROP NOT NULL;

-- 생년월일이 추정값인지 나타낸다. 기존 강아지는 정확한 값으로 간주한다
ALTER TABLE "dogs" ADD COLUMN "birth_date_estimated" BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE "dogs" ALTER COLUMN "birth_date_estimated" DROP DEFAULT;
