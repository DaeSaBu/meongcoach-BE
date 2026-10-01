-- 카테고리 하나는 이용권 하나로 열린다. NULL이면 이용권 없이 볼 수 있는 카테고리다.
ALTER TABLE "training_categories" ADD COLUMN "required_entitlement_type" VARCHAR(50);

-- 환경마다 id가 다를 수 있어 연령대 카테고리를 제목으로 찾는다
UPDATE "training_categories" SET "required_entitlement_type" = 'PUPPY' WHERE "title" = '퍼피 교육';
UPDATE "training_categories" SET "required_entitlement_type" = 'JUNIOR' WHERE "title" = '주니어 교육';
UPDATE "training_categories" SET "required_entitlement_type" = 'ADULT' WHERE "title" = '어덜트 교육';
UPDATE "training_categories" SET "required_entitlement_type" = 'SENIOR' WHERE "title" = '시니어 교육';

-- 이용권이 필요한 카테고리 안에서도 맛보기 커리큘럼은 무료라 커리큘럼마다 유료 여부를 둔다.
-- 기존 행은 기본값 FALSE로 채운 뒤 기본값을 지워 다른 컬럼처럼 값을 직접 넣게 한다
ALTER TABLE "curriculums" ADD COLUMN "is_premium" BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE "curriculums" ALTER COLUMN "is_premium" DROP DEFAULT;

-- 이용권이 필요한 카테고리의 기존 커리큘럼은 모두 유료로 두고, 맛보기로 열 커리큘럼은 운영에서 FALSE로 바꾼다
UPDATE "curriculums" c
SET "is_premium" = TRUE
FROM "topics" t
    JOIN "training_categories" tc ON tc."id" = t."training_category_id"
WHERE c."topic_id" = t."id"
  AND tc."required_entitlement_type" IS NOT NULL;
