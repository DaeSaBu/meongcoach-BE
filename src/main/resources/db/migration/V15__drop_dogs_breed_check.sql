-- 견종을 값객체로 바꾸면서 목록에 없는 견종 직접 입력을 허용하고 등록 견종 상수 이름도 바뀌었다.
-- dev/prod DB는 Flyway 도입 전 ddl-auto가 만든 스키마를 baseline한 것이라 Hibernate가 옛 Breed enum으로 생성한 dogs_breed_check가 남아 있다.
-- V1 기준 스키마에는 없는 제약이고 옛 enum 값만 허용하므로 제거한다. 빈 DB에서는 IF EXISTS로 건너뛴다.
ALTER TABLE "dogs" DROP CONSTRAINT IF EXISTS "dogs_breed_check";
