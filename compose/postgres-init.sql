-- postgres 컨테이너 최초 초기화 시 실행된다 (docker-entrypoint-initdb.d). tmpfs라 재시작마다 다시 실행된다.
-- 마이그레이션 검증(flywayMigrate, FlywaySchemaValidationTest) 전용 DB. 앱(local 프로파일)이 쓰는 meongcoach와 분리해
-- create-drop이 만든 테이블과 flyway_schema_history가 서로 섞이지 않게 한다
CREATE DATABASE meongcoach_schema_check OWNER meongcoach_local;
